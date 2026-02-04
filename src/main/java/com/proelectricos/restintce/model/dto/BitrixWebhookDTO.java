package com.proelectricos.restintce.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BitrixWebhookDTO {
    private String event;
    @JsonProperty("data[FIELDS][ID]")
    private String dealId;
    private String application_token;
}