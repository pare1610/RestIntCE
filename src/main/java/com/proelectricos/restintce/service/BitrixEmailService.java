package com.proelectricos.restintce.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Servicio para enviar correos electrónicos desde Bitrix24
 * con archivos adjuntos obtenidos desde campos personalizados del CRM.
 *
 * Flujo:
 * 1. Obtener datos del contacto y extraer la URL de descarga del archivo.
 * 2. Descargar el archivo y convertirlo a Base64.
 * 3. Construir el payload de crm.activity.add con el archivo adjunto.
 * 4. Enviar el correo mediante la API de Bitrix24.
 */
@Slf4j
@Service
public class BitrixEmailService {

    @Value("${bitrix.webhook.base-url}")
    private String webhookBaseUrl;

    @Value("${bitrix.portal.base-url}")
    private String portalBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envía un correo electrónico desde Bitrix24 con el archivo adjunto
     * obtenido del campo personalizado del contacto.
     *
     * @param fields Mapa con todos los campos del activity (OWNER_ID, OWNER_TYPE_ID,
     *               COMMUNICATIONS, SUBJECT, DESCRIPTION, etc.)
     * @param contactId  ID del contacto del que se obtiene el archivo adjunto.
     * @param fileFieldKey Clave del campo personalizado que contiene el archivo (ej. "UF_CRM_1571435743754").
     * @param attachmentFileName Nombre con el que se adjuntará el archivo (ej. "oferta.pdf").
     */
    public Map<String, Object> sendEmailWithAttachment(
            Map<String, Object> fields,
            String contactId,
            String fileFieldKey,
            String attachmentFileName) {

        // ── Paso 1: Obtener datos del contacto y extraer downloadUrl ────────────
        log.info("Paso 1: Obteniendo datos del contacto ID={} para extraer campo '{}'", contactId, fileFieldKey);
        String downloadUrl = getFileDownloadUrl(contactId, fileFieldKey);

        // ── Paso 2: Descargar el archivo como bytes ──────────────────────────────
        log.info("Paso 2: Descargando archivo desde: {}", downloadUrl);
        byte[] fileBytes = downloadFile(downloadUrl);

        // ── Paso 3: Convertir a Base64 ───────────────────────────────────────────
        log.info("Paso 3: Convirtiendo archivo a Base64 ({} bytes)", fileBytes.length);
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        // ── Paso 4: Construir payload con el archivo adjunto ────────────────────
        log.info("Paso 4: Construyendo payload para crm.activity.add");
        Map<String, Object> enrichedFields = new LinkedHashMap<>(fields);
        enrichedFields.put("FILES", List.of(List.of(attachmentFileName, base64Content)));

        Map<String, Object> payload = Map.of("fields", enrichedFields);

        // ── Paso 5: Enviar petición POST a crm.activity.add ─────────────────────
        log.info("Paso 5: Enviando correo mediante crm.activity.add");
        return postActivity(payload);
    }

    /**
     * Sobrecarga sin archivo adjunto, para enviar el correo tal como viene el payload.
     * Útil cuando el JSON ya viene completo desde el controlador.
     */
    public Map<String, Object> sendEmail(Map<String, Object> payload) {
        log.info("Enviando correo sin adjunto extra (payload directo)");
        return postActivity(payload);
    }

    // ── Métodos privados ──────────────────────────────────────────────────────

    /**
     * Obtiene la URL de descarga del archivo desde el campo personalizado del contacto.
     */
    @SuppressWarnings("unchecked")
    private String getFileDownloadUrl(String contactId, String fileFieldKey) {
        String url = webhookBaseUrl + "crm.contact.get?id=" + contactId;
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.get("result") == null) {
                throw new RuntimeException("No se encontraron datos para el contacto ID: " + contactId);
            }

            Map<String, Object> result = (Map<String, Object>) response.get("result");
            Object fieldValue = result.get(fileFieldKey);

            if (fieldValue == null) {
                throw new RuntimeException("El campo '" + fileFieldKey + "' no existe en el contacto ID: " + contactId);
            }

            // El campo puede ser un objeto directo o un array de objetos
            Map<String, Object> fileData;
            if (fieldValue instanceof List<?> list) {
                if (list.isEmpty()) {
                    throw new RuntimeException("El campo '" + fileFieldKey + "' está vacío en el contacto ID: " + contactId);
                }
                fileData = (Map<String, Object>) list.get(0);
            } else {
                fileData = (Map<String, Object>) fieldValue;
            }

            String downloadUrl = Objects.toString(fileData.get("downloadUrl"), "");
            if (downloadUrl.isBlank()) {
                throw new RuntimeException("No se encontró 'downloadUrl' en el campo '" + fileFieldKey + "'");
            }

            return downloadUrl;

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al obtener contacto {}: {}", e.getStatusCode(), contactId, e.getResponseBodyAsString());
            throw new RuntimeException("Error al obtener contacto de Bitrix24 (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        }
    }

    /**
     * Descarga el archivo desde la URL relativa o absoluta.
     * Si la URL es relativa (comienza con /), se concatena con el dominio base del portal.
     */
    private byte[] downloadFile(String downloadUrl) {
        // Si la URL es relativa, concatenar con el dominio base del portal
        String fullUrl = downloadUrl.startsWith("http") ? downloadUrl : portalBaseUrl + downloadUrl;
        log.info("URL completa de descarga: {}", fullUrl);
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fullUrl, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Respuesta inválida al descargar archivo: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al descargar archivo: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error al descargar archivo desde Bitrix24 (HTTP " + e.getStatusCode() + ")");
        }
    }

    /**
     * Realiza la petición POST a crm.activity.add para crear la actividad de correo.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postActivity(Map<String, Object> payload) {
        String url = webhookBaseUrl + "crm.activity.add";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Spring convierte el Map a JSON automáticamente con MappingJackson2HttpMessageConverter
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Correo enviado correctamente. Respuesta: {}", response.getBody());
                return response.getBody();
            }

            throw new RuntimeException("Respuesta inesperada de crm.activity.add: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} en crm.activity.add: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error en crm.activity.add (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al enviar payload: {}", e.getMessage());
            throw new RuntimeException("Error al enviar actividad a Bitrix24: " + e.getMessage());
        }
    }
}

