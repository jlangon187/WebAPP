package com.gpbmods.app.data.remote.dto;

public class EncryptionOverviewDto {
    public int pending;
    public int running;
    public int done;
    public int failed;
    public int doneWithoutNotification;
    public boolean mailConfigured;
    public String mailHost;
}
