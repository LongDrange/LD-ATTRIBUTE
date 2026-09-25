package com.longdrange.ldattribute.core.storage;

import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.util.UUID;

public interface StorageProvider {
    void init() throws Exception;
    void shutdown();
    PlayerModuleData load(UUID uuid, String module);
    void save(UUID uuid, String module, PlayerModuleData data);
    void delete(UUID uuid, String module);
    boolean exists(UUID uuid, String module);
    StorageType getType();
}