package com.proelectricos.restintce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class BitrixApiService {

    @Value("${bitrix.webhook.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getDealData(String dealId) {
        // 1. Obtener datos del Deal
        String urlDeal = baseUrl + "crm.deal.get?id=" + dealId;
        Map<String, Object> responseDeal = restTemplate.getForObject(urlDeal, Map.class);

        if (responseDeal == null || responseDeal.get("result") == null) {
            throw new RuntimeException("No se encontraron datos para el Deal: " + dealId);
        }

        // Hacemos una copia mutable de los datos del deal
        Map<String, Object> dealData = new HashMap<>((Map<String, Object>) responseDeal.get("result"));

        // 2. Datos de la compañia
        if (dealData.get("COMPANY_ID") != null && !Objects.toString(dealData.get("COMPANY_ID")).equals("0")) {
            String companyId = dealData.get("COMPANY_ID").toString();
            Map<String, Object> companyData = getEntityData("crm.company.get", companyId);

            // Mapeamos el nombre de la compañía
            if (companyData != null) {
                dealData.put("COMPANY_NAME_FETCHED", companyData.get("TITLE"));
                dealData.put("COMPANY_PHONE_FETCHED", extractPhone(companyData));
            }
        }

        // 3. Datos del contacto
        if (dealData.get("CONTACT_ID") != null && !Objects.toString(dealData.get("CONTACT_ID")).equals("0")) {
            String contactId = dealData.get("CONTACT_ID").toString();
            Map<String, Object> contactData = getEntityData("crm.contact.get", contactId);

            if (contactData != null) {
                String fullName = Objects.toString(contactData.get("NAME"), "") + " " +
                        Objects.toString(contactData.get("LAST_NAME"), "");
                dealData.put("CONTACT_NAME_FETCHED", fullName.trim());
            }
        }

        //4. Datos del usuario
        if (dealData.get("ASSIGNED_BY_ID") != null && !Objects.toString(dealData.get("ASSIGNED_BY_ID")).equals("0")) {
            String contactId = dealData.get("ASSIGNED_BY_ID").toString();
            Map<String, Object> contactData = getEntityData("user.get", contactId);

            if (contactData != null) {
                String fullName = Objects.toString(contactData.get("NAME"), "") + " " +
                        Objects.toString(contactData.get("LAST_NAME"), "");
                dealData.put("USER_NAME_FETCHED", fullName.trim());
            }
        }

        return dealData;
    }

    // Método genérico para traer entidades (Company o Contact)
    private Map<String, Object> getEntityData(String method, String id) {
        try {
            String url = baseUrl + method + "?id=" + id;
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("result") != null) {
                return (Map<String, Object>) response.get("result");
            }
        } catch (Exception e) {
            // Loguear error pero no detener el flujo principal (Fail safe)
            System.err.println("Error fetching " + method + " for ID " + id + ": " + e.getMessage());
        }
        return null;
    }

    // Helper para extraer teléfono (Bitrix devuelve una lista de teléfonos)
    private String extractPhone(Map<String, Object> data) {
        if (data.containsKey("PHONE") && data.get("PHONE") instanceof java.util.List) {
            java.util.List<Map<String, Object>> phones = (java.util.List) data.get("PHONE");
            if (!phones.isEmpty()) {
                return Objects.toString(phones.get(0).get("VALUE"), "");
            }
        }
        return "";
    }
}