package com.gpbmods.app.data.remote.dto;

public class AdminStatsResponse {
    public double totalSales;
    public long totalSalesCount;
    public long salesCountLast30;
    public long totalUsers;
    public long newUsers;
    public long totalTickets;
    public long activeTickets;
    public long closedTickets;
    public long respondedTickets;
    public long openTickets;
    public long totalMods;
    public long featuredMods;
    public NasDto nas;

    public static class NasDto {
        public boolean online;
        public long usedBytes;
        public long totalBytes;
        public int usagePercent;
        public long homeImagesCount;
        public long modsFilesCount;
        public String homeImagesPath;
        public String modsFilesPath;
    }
}
