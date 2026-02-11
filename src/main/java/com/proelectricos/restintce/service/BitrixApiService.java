package com.proelectricos.restintce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BitrixApiService {

    @Value("${bitrix.webhook.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getDealData(String dealId) {
        // 1. Obtener datos base del Deal
        String url = baseUrl + "crm.deal.get?id=" + dealId;
        Map response = restTemplate.getForObject(url, Map.class);

        if (response == null || response.get("result") == null) {
            throw new RuntimeException("No se encontraron datos para el Deal: " + dealId);
        }

        // Crear mapa mutable con los datos originales
        Map<String, Object> dealData = new HashMap<>((Map<String, Object>) response.get("result"));

        // --- ENRIQUECIMIENTO DE DATOS ---

        // 2. COMPAÑÍA (Cliente Empresa)
        String companyId = Objects.toString(dealData.get("COMPANY_ID"), "0");
        if (!"0".equals(companyId) && !companyId.isEmpty()) {
            Map<String, Object> companyInfo = getEntity("crm.company.get", companyId);
            if (companyInfo != null) {
                dealData.put("COMPANY_NAME_FETCHED", companyInfo.get("TITLE"));
            }
        }

        // 3. CONTACTO (Cliente Persona)
        String contactId = Objects.toString(dealData.get("CONTACT_ID"), "0");
        if (!"0".equals(contactId) && !contactId.isEmpty()) {
            Map<String, Object> contactInfo = getEntity("crm.contact.get", contactId);
            if (contactInfo != null) {
                String fullName = Objects.toString(contactInfo.get("NAME"), "") + " " +
                        Objects.toString(contactInfo.get("LAST_NAME"), "");
                dealData.put("CONTACT_NAME_FETCHED", fullName.trim());
            }
        }

        // 4. USUARIO RESPONSABLE (Vendedor)
        String assignedById = Objects.toString(dealData.get("ASSIGNED_BY_ID"), "0");
        if (!"0".equals(assignedById) && !assignedById.isEmpty()) {
            // NOTA: user.get devuelve una lista, aunque filtremos por ID
            Map<String, Object> userInfo = getUserData(assignedById);
            if (userInfo != null) {
                // Nombre completo
                String userName = Objects.toString(userInfo.get("NAME"), "") + " " +
                        Objects.toString(userInfo.get("LAST_NAME"), "");
                dealData.put("RESPONSIBLE_NAME", userName.trim());

                // Correo
                dealData.put("RESPONSIBLE_EMAIL", Objects.toString(userInfo.get("EMAIL"), "Sin Email"));

                // Teléfono (Priorizamos Celular Personal, sino Teléfono Trabajo)
                String phone = Objects.toString(userInfo.get("PERSONAL_MOBILE"), "");
                if (phone.isEmpty()) {
                    phone = Objects.toString(userInfo.get("WORK_PHONE"), "");
                }
                dealData.put("RESPONSIBLE_PHONE", phone);
            }
        }

        return dealData;
    }

    // Método genérico para entidades CRM (Company/Contact)
    private Map<String, Object> getEntity(String method, String id) {
        try {
            String url = baseUrl + method + "?id=" + id;
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("result") != null) {
                return (Map<String, Object>) response.get("result");
            }
        } catch (Exception e) {
            System.err.println("Warning: Error fetching " + method + " ID: " + id);
        }
        return null;
    }

    // Método específico para Usuarios (user.get)
    private Map<String, Object> getUserData(String userId) {
        try {
            // user.get requiere formato ID=123 (no id=123 como CRM) o filtro FILTER[ID]=123
            // La forma más simple compatible es ?ID=...
            String url = baseUrl + "user.get?ID=" + userId;
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("result") != null) {
                Object resultObj = response.get("result");
                // Bitrix devuelve una lista de usuarios en user.get
                if (resultObj instanceof List) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) resultObj;
                    if (!list.isEmpty()) {
                        return list.get(0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Error fetching user ID: " + userId);
        }
        return null;
    }
}