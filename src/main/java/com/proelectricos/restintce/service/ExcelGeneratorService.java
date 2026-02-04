package com.proelectricos.restintce.service;

import org.springframework.beans.factory.annotation.Value;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

@Service
public class ExcelGeneratorService {

    @Value("${app.excel.template-path}")
    private String templatePath;

    public void createExcelFromTemplate(Map<String, Object> dealData, String targetPath) throws Exception {
        // 1. Cargar la plantilla desde src/main/resources
        Resource resource = new ClassPathResource(templatePath);

        try (InputStream is = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 2. Mapeo de datos según requerimiento
            // TITLE (Nombre del negocio) -> Celda C8
            updateCell(sheet, "C8", Objects.toString(dealData.get("TITLE"), "Sin Nombre"));

            // OPPORTUNITY (Monto) -> Celda B3
            // Nota: Se intenta convertir a numérico para que Excel lo reconozca como dinero
            updateCellNumeric(sheet, "B3", Objects.toString(dealData.get("OPPORTUNITY"), "0"));

            // COMMENTS (Observaciones) -> Celda A10
            updateCell(sheet, "A10", Objects.toString(dealData.get("COMMENTS"), "Sin observaciones"));

            // 3. Guardar el nuevo archivo en la ruta de exportación
            try (FileOutputStream os = new FileOutputStream(targetPath)) {
                workbook.write(os);
            }
        }
    }

    private void updateCell(Sheet sheet, String cellReference, String value) {
        CellReference ref = new CellReference(cellReference);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) row = sheet.createRow(ref.getRow());
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) cell = row.createCell(ref.getCol());

        cell.setCellValue(value);
    }

    // Método adicional para manejar montos como números y no como texto
    private void updateCellNumeric(Sheet sheet, String cellReference, String value) {
        CellReference ref = new CellReference(cellReference);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) row = sheet.createRow(ref.getRow());
        Cell cell = row.getCell(ref.getCol());
        if (cell == null) cell = row.createCell(ref.getCol());

        try {
            double numericValue = Double.parseDouble(value);
            cell.setCellValue(numericValue);
        } catch (NumberFormatException e) {
            cell.setCellValue(value);
        }
    }
}