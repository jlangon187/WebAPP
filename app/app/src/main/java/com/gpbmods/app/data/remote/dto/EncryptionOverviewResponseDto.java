package com.gpbmods.app.data.remote.dto;

import java.util.List;

public class EncryptionOverviewResponseDto {
    public int pending;
    public int running;
    public int done;
    public int failed;
    public int doneWithoutNotification;
    public int failedWithError;
    public boolean mailConfigured;
    public String mailHost;
    public String mailUsername;
    public List<EncryptionJobOverviewItemDto> recent;
}
