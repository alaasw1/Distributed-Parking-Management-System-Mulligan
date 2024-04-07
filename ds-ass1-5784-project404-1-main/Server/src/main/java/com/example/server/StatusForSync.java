package com.example.server;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class StatusForSync {
    private static final AtomicBoolean isServerStarted = new AtomicBoolean(false);
    private static final AtomicBoolean isLoadDatabaseDone = new AtomicBoolean(false);
    private static final AtomicReference<String> lastSyncTimeStr = new AtomicReference<>("1000-01-01T01:01:01");

    public static boolean getIsServerStarted() {
        return isServerStarted.get();
    }

    public static void setIsServerStarted(boolean isServerStarted) {
        StatusForSync.isServerStarted.set(isServerStarted);
    }

    public static boolean getIsLoadDatabaseDone() {
        return isLoadDatabaseDone.get();
    }

    public static void setIsLoadDatabaseDone(boolean isLoadDatabaseDone) {
        StatusForSync.isLoadDatabaseDone.set(isLoadDatabaseDone);
    }

    public static String getLastSyncTimeStr() {
        return lastSyncTimeStr.get();
    }

    public static void setLastSyncTimeStr(String lastSyncTimeStr) {
        StatusForSync.lastSyncTimeStr.set(lastSyncTimeStr);
    }
}
