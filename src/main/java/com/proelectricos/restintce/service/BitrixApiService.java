package com.proelectricos.restintce.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class BitrixApiService {

    @Value("${bitrix.webhook.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getDealData(String dealId) {
        // 1. Obtener datos base del Deal
        Map<String, Object> dealData = fetchDealBase(dealId);

        // 2. Enriquecer con datos de Compañía
        enrichCompanyData(dealData);

        // 3. Enriquecer con datos de Contacto
        enrichContactData(dealData);

        // 4. Enriquecer con datos del Responsable
        enrichResponsibleData(dealData);

        return dealData;
    }

    // ── Métodos de enriquecimiento ──────────────────────────────────

    private Map<String, Object> fetchDealBase(String dealId) {
        String url = baseUrl + "crm.deal.get?id=" + dealId;

        try {
            Map response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("result") == null) {
                throw new RuntimeException("No se encontraron datos para el Deal: " + dealId);
            }
            return new HashMap<>((Map<String, Object>) response.get("result"));

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Credenciales de Bitrix inválidas. Regenera el webhook en Bitrix24 y actualiza 'bitrix.webhook.base-url' en application.properties");
            throw new RuntimeException("Credenciales de Bitrix24 inválidas o expiradas. Regenera el webhook en Bitrix24.");

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("Sin permisos para acceder al Deal {}. Verifica los permisos del webhook.", dealId);
            throw new RuntimeException("Sin permisos en Bitrix24 para acceder al Deal: " + dealId);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al consultar Bitrix24: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al consultar Bitrix24 (HTTP " + e.getStatusCode() + ")");
        }
    }

    private void enrichCompanyData(Map<String, Object> dealData) {
        String companyId = safeGet(dealData, "COMPANY_ID");
        if (isValidId(companyId)) {
            Map<String, Object> companyInfo = getEntity("crm.company.get", companyId);
            if (companyInfo != null) {
                dealData.put("COMPANY_NAME_FETCHED", companyInfo.get("TITLE"));
            }
        }
    }

    private void enrichContactData(Map<String, Object> dealData) {
        String contactId = safeGet(dealData, "CONTACT_ID");
        if (isValidId(contactId)) {
            Map<String, Object> contactInfo = getEntity("crm.contact.get", contactId);
            if (contactInfo != null) {
                String fullName = safeGet(contactInfo, "NAME") + " " + safeGet(contactInfo, "LAST_NAME");
                dealData.put("CONTACT_NAME_FETCHED", fullName.trim());
            }
        }
    }

    private void enrichResponsibleData(Map<String, Object> dealData) {
        String assignedById = safeGet(dealData, "ASSIGNED_BY_ID");
        if (!isValidId(assignedById)) return;

        Map<String, Object> userInfo = getUserData(assignedById);
        if (userInfo == null) return;

        String userName = safeGet(userInfo, "NAME") + " " + safeGet(userInfo, "LAST_NAME");
        dealData.put("RESPONSIBLE_NAME", userName.trim());
        dealData.put("RESPONSIBLE_EMAIL", safeGet(userInfo, "EMAIL", "Sin Email"));

        String phone = safeGet(userInfo, "PERSONAL_MOBILE");
        if (phone.isEmpty()) {
            phone = safeGet(userInfo, "WORK_PHONE");
        }
        dealData.put("RESPONSIBLE_PHONE", phone);
    }

    // ── Métodos de acceso a API ─────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getEntity(String method, String id) {
        try {
            String url = baseUrl + method + "?id=" + id;
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("result") != null) {
                return (Map<String, Object>) response.get("result");
            }
        } catch (Exception e) {
            log.warn("Error al obtener {} con ID {}: {}", method, id, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserData(String userId) {
        try {
            String url = baseUrl + "user.get?ID=" + userId;
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("result") instanceof List<?> list && !list.isEmpty()) {
                return (Map<String, Object>) list.get(0);
            }
        } catch (Exception e) {
            log.warn("Error al obtener usuario con ID {}: {}", userId, e.getMessage());
        }
        return null;
    }

    // ── Utilidades ──────────────────────────────────────────────────

    private String safeGet(Map<String, Object> map, String key) {
        return Objects.toString(map.get(key), "");
    }

    private String safeGet(Map<String, Object> map, String key, String defaultValue) {
        return Objects.toString(map.get(key), defaultValue);
    }

    private boolean isValidId(String id) {
        return id != null && !id.isEmpty() && !"0".equals(id);
    }
}