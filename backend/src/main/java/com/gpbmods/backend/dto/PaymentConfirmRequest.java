package com.gpbmods.backend.dto;

import java.util.List;

public class PaymentConfirmRequest {
    private String provider;
    private String externalId;
    private List<Long> modIds;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public List<Long> getModIds() { return modIds; }
    public void setModIds(List<Long> modIds) { this.modIds = modIds; }
}
