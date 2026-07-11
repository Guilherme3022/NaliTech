package com.nalitech.shared.storage;

public interface StorageService {

    String store(String key, byte[] content, String contentType);

    byte[] retrieve(String key);

    void delete(String key);
}
