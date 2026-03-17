package com.proelectricos.restintce.controller;

import com.proelectricos.restintce.service.BitrixApiService;
import com.proelectricos.restintce.service.ExcelGeneratorService;
import com.proelectricos.restintce.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/v1/bitrix")
@RequiredArgsConstructor
public class BitrixController {

    private final BitrixApiService bitrixService;
    private final ExcelGeneratorService excelService;
    private final GoogleDriveService driveService;

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

            // 2. Generar archivo Excel local
            String fileName = "TAB-" + dealId + "-26.xlsx";
            String fullPath = exportPath + fileName;
            excelService.createExcelFromTemplate(dealData, fullPath);

            // 3. Crear carpeta en Google Drive
            String dealTitle = Objects.toString(dealData.getOrDefault("TITLE", "Sin Nombre"));
            String folderName = "TAB-" + dealId + "-26 " + dealTitle;
            String folderId = driveService.createFolder(folderName, driveRootFolderId);

            // 4. Subir archivo a la carpeta en Drive
            String webViewLink = driveService.uploadFile(fullPath, fileName, folderId);

            log.info(">> Proceso completado. Archivo en Drive: {}", webViewLink);
            return ResponseEntity.ok("Archivo generado y subido con éxito: " + webViewLink);

        } catch (Exception e) {
            log.error(">> Error en el flujo de generación: ", e);
            return ResponseEntity.internalServerError().body("Error técnico: " + e.getMessage());
        }
    }
}