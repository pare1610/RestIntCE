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

            // --- DATOS DEL NEGOCIO ---
            updateCell(sheet, "C10", Objects.toString(dealData.get("TITLE"), "Sin Nombre"));
            //updateCellNumeric(sheet, "B3", Objects.toString(dealData.get("OPPORTUNITY"), "0"));
            //updateCell(sheet, "A10", Objects.toString(dealData.get("COMMENTS"), "Sin observaciones"));
            updateCell(sheet, "C10", Objects.toString("TAB-"+dealData.get("ID")+"-26", "Sin Nombre"));

            // --- DATOS DEL CLIENTE (Compañía / Contacto) ---
            String companyName = dealData.containsKey("COMPANY_NAME_FETCHED")
                    ? dealData.get("COMPANY_NAME_FETCHED").toString()
                    : "Sin Compañía";
            updateCell(sheet, "C8", companyName);

            String contactName = dealData.containsKey("CONTACT_NAME_FETCHED")
                    ? dealData.get("CONTACT_NAME_FETCHED").toString()
                    : "Sin Contacto";
            updateCell(sheet, "C12", contactName);

            // --- NUEVOS CAMPOS: DATOS DEL RESPONSABLE (VENDEDOR) ---

            // Nombre del Asesor
            String responsibleName = dealData.containsKey("RESPONSIBLE_NAME")
                    ? dealData.get("RESPONSIBLE_NAME").toString()
                    : "Sin Asignar";
            updateCell(sheet, "C28", responsibleName);

            // Correo del Asesor
            String responsibleEmail = dealData.containsKey("RESPONSIBLE_EMAIL")
                    ? dealData.get("RESPONSIBLE_EMAIL").toString()
                    : "";
            updateCell(sheet, "C30", responsibleEmail);

            // Teléfono del Asesor
            String responsiblePhone = dealData.containsKey("RESPONSIBLE_PHONE")
                    ? dealData.get("RESPONSIBLE_PHONE").toString()
                    : "";
            updateCell(sheet, "C32", responsiblePhone);

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