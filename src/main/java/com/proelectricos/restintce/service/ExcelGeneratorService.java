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
        Resource resource = new ClassPathResource(templatePath);

        try (InputStream is = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // --- MAPEO ORIGINAL ---
            updateCell(sheet, "C10", Objects.toString(dealData.get("TITLE"), "Sin Nombre"));
            //updateCellNumeric(sheet, "B3", Objects.toString(dealData.get("OPPORTUNITY"), "0"));
            //updateCell(sheet, "A10", Objects.toString(dealData.get("COMMENTS"), "Sin observaciones"));
            updateCell(sheet, "F5", Objects.toString("TAB-"+dealData.get("ID")+"-26", "Sin observaciones"));
            // --- NUEVOS CAMPOS (AJUSTE PARA PRUEBAS) ---

            // Nombre de la Compañía -> Celda C8
            String companyName = dealData.containsKey("COMPANY_NAME_FETCHED")
                    ? dealData.get("COMPANY_NAME_FETCHED").toString()
                    : "Sin Compañía";
            updateCell(sheet, "C8", companyName);

            // Nombre del Contacto -> Celda C12
            String contactName = dealData.containsKey("CONTACT_NAME_FETCHED")
                    ? dealData.get("CONTACT_NAME_FETCHED").toString()
                    : "Sin Contacto";
            updateCell(sheet, "C12", contactName);

            // Nombre del Contacto -> Celda C28
            String userName = dealData.containsKey("USER_NAME_FETCHED")
                    ? dealData.get("USER_NAME_FETCHED").toString()
                    : "Sin Contacto";
            updateCell(sheet, "C28", contactName);

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