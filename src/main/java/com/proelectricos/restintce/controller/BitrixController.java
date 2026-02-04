package com.proelectricos.restintce.controller;

import com.proelectricos.restintce.service.BitrixApiService;
import com.proelectricos.restintce.service.ExcelGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bitrix")
public class BitrixController {

    private static final Logger log = LoggerFactory.getLogger(BitrixController.class);

    @Autowired
    private BitrixApiService bitrixService;

    @Autowired
    private ExcelGeneratorService excelService;

    @Value("${app.excel.export-path}")
    private String exportPath;

    // Cambiamos a APPLICATION_JSON_VALUE porque así está en tu captura de Bitrix
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> process(@RequestBody Map<String, Object> payload) {
        try {
            // Extraemos el ID del JSON: { "id_negocio": "123" }
            if (!payload.containsKey("id_negocio")) {
                return ResponseEntity.badRequest().body("Error: No se recibió el campo 'id_negocio'");
            }

            String dealId = payload.get("id_negocio").toString();
            log.info(">>>> Iniciando generación de Excel para Deal ID: {}", dealId);

            // 1. Obtener datos detallados del Deal desde la API de Proeléctricos
            Map<String, Object> dealData = bitrixService.getDealData(dealId);

            // 2. Definir nombre y ruta del archivo
            String fileName = "TAB-" + dealId + "-26.xlsx";
            String fullPath = exportPath + fileName;

            // 3. Generar el archivo inyectando los datos en la plantilla
            excelService.createExcelFromTemplate(dealData, fullPath);

            log.info(">>>> Archivo generado exitosamente en: {}", fullPath);
            return ResponseEntity.ok("Archivo generado con éxito: " + fileName);

        } catch (Exception e) {
            log.error(">>>> Error en el flujo de generación: ", e);
            return ResponseEntity.internalServerError().body("Error técnico: " + e.getMessage());
        }
    }
}