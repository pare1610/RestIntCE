package com.proelectricos.restintce.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Servicio encargado exclusivamente de generar archivos Excel
 * a partir de una plantilla y datos de Bitrix.
 */
@Slf4j
@Service
public class ExcelGeneratorService {

    @Value("${app.excel.template-path}")
    private String templatePath;

    /**
     * Genera un archivo Excel inyectando datos del deal en la plantilla.
     *
     * @param dealData   mapa con los datos del deal de Bitrix
     * @param targetPath ruta completa donde se guardará el archivo generado
     */
    public void createExcelFromTemplate(Map<String, Object> dealData, String targetPath) throws Exception {
        Resource resource = new ClassPathResource(templatePath);

        try (InputStream is = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // --- DATOS DEL NEGOCIO ---
            String dealRef = "TAB-" + dealData.get("ID") + "-26";
            updateCell(sheet, "C10", dealRef);

            // --- DATOS DEL CLIENTE ---
            updateCell(sheet, "C8", getValueOrDefault(dealData, "COMPANY_NAME_FETCHED", "Sin Compañía"));
            updateCell(sheet, "C12", getValueOrDefault(dealData, "CONTACT_NAME_FETCHED", "Sin Contacto"));

            // --- DATOS DEL RESPONSABLE (VENDEDOR) ---
            updateCell(sheet, "C28", getValueOrDefault(dealData, "RESPONSIBLE_NAME", "Sin Asignar"));
            updateCell(sheet, "C30", getValueOrDefault(dealData, "RESPONSIBLE_EMAIL", ""));
            updateCell(sheet, "C32", getValueOrDefault(dealData, "RESPONSIBLE_PHONE", ""));

            try (FileOutputStream os = new FileOutputStream(targetPath)) {
                workbook.write(os);
            }

            log.info("Excel generado exitosamente en: {}", targetPath);
        }
    }

    // ── Utilidades privadas ──────────────────────────────────────────

    private String getValueOrDefault(Map<String, Object> data, String key, String defaultValue) {
        return data.containsKey(key) ? data.get(key).toString() : defaultValue;
    }

    private void updateCell(Sheet sheet, String cellReference, String value) {
        CellReference ref = new CellReference(cellReference);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) row = sheet.createRow(ref.getRow());
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) cell = row.createCell(ref.getCol());
        cell.setCellValue(value);
    }

    @SuppressWarnings("unused")
    private void updateCellNumeric(Sheet sheet, String cellReference, String value) {
        CellReference ref = new CellReference(cellReference);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) row = sheet.createRow(ref.getRow());
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) cell = row.createCell(ref.getCol());
        try {
            cell.setCellValue(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            cell.setCellValue(value);
        }
    }
}
