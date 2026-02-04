package com.proelectricos.restintce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BitrixApiService {

    @Value("${bitrix.webhook.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getDealData(String dealId) {
        // Método crm.deal.get de la REST API
        String url = baseUrl + "crm.deal.get?id=" + dealId;

        var response = restTemplate.getForObject(url, Map.class);
        if (response != null && response.get("result") != null) {
            return (Map<String, Object>) response.get("result");
        }
        throw new RuntimeException("No se encontraron datos para el Deal: " + dealId);
    }
}