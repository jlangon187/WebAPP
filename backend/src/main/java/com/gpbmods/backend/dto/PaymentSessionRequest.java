package com.gpbmods.backend.dto;

import java.util.List;

public class PaymentSessionRequest {
    private String provider;
    private List<Long> modIds;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<Long> getModIds() {
        return modIds;
    }

    public void setModIds(List<Long> modIds) {
        this.modIds = modIds;
    }
}
