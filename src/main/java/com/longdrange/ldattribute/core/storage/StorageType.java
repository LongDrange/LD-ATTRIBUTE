package com.longdrange.ldattribute.core.storage;

public enum StorageType {
    YAML, SQLITE, MYSQL;

    public static StorageType fromString(String s) {
        if (s == null) return YAML;
        try { return valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}