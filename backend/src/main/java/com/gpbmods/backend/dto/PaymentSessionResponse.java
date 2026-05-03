package com.gpbmods.backend.dto;

public class PaymentSessionResponse {
    private String provider;
    private String redirectUrl;
    private String externalId;
    private String message;

    public PaymentSessionResponse() {}

    public PaymentSessionResponse(String provider, String redirectUrl, String externalId, String message) {
        this.provider = provider;
        this.redirectUrl = redirectUrl;
        this.externalId = externalId;
        this.message = message;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
