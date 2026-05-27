package com.proelectricos.restintce.controller;

import com.proelectricos.restintce.service.BitrixApiService;
import com.proelectricos.restintce.service.BitrixEmailService;
import com.proelectricos.restintce.service.ExcelGeneratorService;
import com.proelectricos.restintce.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/v1/bitrix")
@RequiredArgsConstructor
public class BitrixController {

    private static final String FILE_EXTENSION = ".xlsx";

    private final BitrixApiService bitrixService;
    private final ExcelGeneratorService excelService;
    private final GoogleDriveService driveService;
    private final BitrixEmailService emailService;

    @Value("${app.excel.export-path}")
    private String exportPath;

    @Value("${google.drive.root-folder-id}")
    private String driveRootFolderId;

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> process(@RequestBody Map<String, Object> payload) {
        try {
            if (!payload.containsKey("id_negocio")) {
                return ResponseEntity.badRequest().body("Error: No se recibió el campo 'id_negocio'");
            }

            String dealId = payload.get("id_negocio").toString();
            log.info(">> Iniciando generación de Excel para Deal ID: {}", dealId);

            // 1. Obtener datos del Deal desde Bitrix
            Map<String, Object> dealData = bitrixService.getDealData(dealId);

            String baseFileName = "TAB-" + dealId + "-26";

            // 2. Crear carpeta principal en Google Drive (si existe, se reutiliza)
            String dealTitle = Objects.toString(dealData.getOrDefault("TITLE", "Sin Nombre"));
            String folderName = baseFileName + " " + dealTitle;
            String folderId = driveService.createFolder(folderName, driveRootFolderId);

            // 3. Crear subcarpetas estándar (OFERTA, PROVEEDOR, etc.) y obtener ID de OFERTA
            String ofertaFolderId = driveService.createSubFolders(folderId);

            // 4. Resolver nombre del archivo y subir a OFERTA
            String webViewLink = resolveAndUpload(baseFileName, ofertaFolderId, folderId, dealData);

            log.info(">> Proceso completado. Archivo en Drive: {}", webViewLink);
            return ResponseEntity.ok("Archivo generado y subido con éxito: " + webViewLink);

        } catch (Exception e) {
            log.error(">> Error en el flujo de generación: ", e);
            return ResponseEntity.internalServerError().body("Error técnico: " + e.getMessage());
        }
    }

    /**
     * Determina el nombre del archivo destino y:
     * - Si es el primero (TAB-XXXXX-26.xlsx): genera desde plantilla y sube a OFERTA.
     * - Si ya existe (en OFERTA o en carpeta principal): copia el último archivo en Drive con el siguiente nombre ACT.
     */
    private String resolveAndUpload(String baseFileName, String ofertaFolderId, String mainFolderId, Map<String, Object> dealData) throws Exception {
        String baseFile = baseFileName + FILE_EXTENSION;

        // Buscar archivo base primero en OFERTA, luego en carpeta principal (compatibilidad)
        String sourceFileId = driveService.findFileIdByName(ofertaFolderId, baseFile);
        if (sourceFileId == null) {
            sourceFileId = driveService.findFileIdByName(mainFolderId, baseFile);
            if (sourceFileId != null) {
                log.info("Archivo base encontrado en carpeta principal (no en OFERTA), se usará como origen para copias.");
            }
        }

        // Caso 1: no existe ningún archivo → generar desde plantilla y subir a OFERTA
        if (sourceFileId == null) {
            String fullPath = Path.of(exportPath, baseFile).toString();
            excelService.createExcelFromTemplate(dealData, fullPath);
            return driveService.uploadFile(fullPath, baseFile, ofertaFolderId);
        }

        // Caso 2: ya existe → buscar el último ACT (en OFERTA) y copiar
        int actCounter = 1;
        String lastExistingId = sourceFileId;

        while (true) {
            String actName = baseFileName + " ACT" + actCounter + FILE_EXTENSION;
            String actId = driveService.findFileIdByName(ofertaFolderId, actName);
            if (actId == null) {
                String prevName = actCounter == 1 ? baseFile : baseFileName + " ACT" + (actCounter - 1) + FILE_EXTENSION;
                log.info("Copiando '{}' como '{}'", prevName, actName);
                return driveService.copyFile(lastExistingId, actName, ofertaFolderId);
            }
            lastExistingId = actId;
            actCounter++;
        }
    }

    // ── Endpoints de correo electrónico ──────────────────────────────────────

    /**
     * Envía un correo electrónico desde Bitrix24 adjuntando el archivo
     * almacenado en el campo personalizado del contacto.
     *
     * Parámetros adicionales esperados en el body JSON (además de "fields"):
     *   - contact_id      : ID del contacto del que se obtiene el archivo.
     *   - file_field_key  : Clave del campo personalizado (ej. "UF_CRM_1571435743754").
     *   - attachment_name : Nombre del archivo adjunto (ej. "oferta.pdf"). Opcional, default "adjunto.pdf".
     *
     * Ejemplo de body:
     * {
     *   "contact_id": "125",
     *   "file_field_key": "UF_CRM_1571435743754",
     *   "attachment_name": "TAB-70584-26.pdf",
     *   "fields": { ... }
     * }
     */
    @PostMapping(value = "/send-email", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendEmailWithAttachment(@RequestBody Map<String, Object> payload) {
        try {
            String contactId = Objects.toString(payload.get("contact_id"), "");
            String fileFieldKey = Objects.toString(payload.get("file_field_key"), "");
            String attachmentName = Objects.toString(payload.getOrDefault("attachment_name", "adjunto.pdf"), "adjunto.pdf");

            if (contactId.isBlank()) {
                return ResponseEntity.badRequest().body("Error: Se requiere el campo 'contact_id'");
            }
            if (fileFieldKey.isBlank()) {
                return ResponseEntity.badRequest().body("Error: Se requiere el campo 'file_field_key'");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) payload.get("fields");
            if (fields == null) {
                return ResponseEntity.badRequest().body("Error: Se requiere el objeto 'fields'");
            }

            log.info(">> Enviando correo con adjunto. Contacto={}, Campo={}, Archivo={}", contactId, fileFieldKey, attachmentName);
            Map<String, Object> result = emailService.sendEmailWithAttachment(fields, contactId, fileFieldKey, attachmentName);
            return ResponseEntity.ok("Correo enviado correctamente. ID de actividad: " + result.get("result"));

        } catch (Exception e) {
            log.error(">> Error al enviar correo con adjunto: ", e);
            return ResponseEntity.internalServerError().body("Error técnico: " + e.getMessage());
        }
    }

    /**
     * Envía un correo electrónico desde Bitrix24 usando el payload tal como viene.
     * No adjunta ningún archivo adicional. Útil para pruebas o cuando el JSON
     * ya contiene FILES con el Base64 listo.
     *
     * Body esperado: { "fields": { ... } }
     */
    @PostMapping(value = "/send-email-simple", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendEmailSimple(@RequestBody Map<String, Object> payload) {
        try {
            if (!payload.containsKey("fields")) {
                return ResponseEntity.badRequest().body("Error: Se requiere el objeto 'fields'");
            }
            log.info(">> Enviando correo (modo simple) sin adjunto dinámico");
            Map<String, Object> result = emailService.sendEmail(payload);
            return ResponseEntity.ok("Correo enviado correctamente. ID de actividad: " + result.get("result"));

        } catch (Exception e) {
            log.error(">> Error al enviar correo simple: ", e);
            return ResponseEntity.internalServerError().body("Error técnico: " + e.getMessage());
        }
    }
}