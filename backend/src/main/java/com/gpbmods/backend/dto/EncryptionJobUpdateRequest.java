package com.gpbmods.backend.dto;

public class EncryptionJobUpdateRequest {
    private String outputRelativePath;
    private String errorMessage;

    public String getOutputRelativePath() {
        return outputRelativePath;
    }

    public void setOutputRelativePath(String outputRelativePath) {
        this.outputRelativePath = outputRelativePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
