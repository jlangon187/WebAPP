package com.gpbmods.backend.dto;

public class PrepareDownloadResponse {
    private Long jobId;
    private String status;
    private String message;
    private String downloadToken;

    public PrepareDownloadResponse() {}

    public PrepareDownloadResponse(Long jobId, String status, String message, String downloadToken) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.downloadToken = downloadToken;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDownloadToken() { return downloadToken; }
    public void setDownloadToken(String downloadToken) { this.downloadToken = downloadToken; }
}
