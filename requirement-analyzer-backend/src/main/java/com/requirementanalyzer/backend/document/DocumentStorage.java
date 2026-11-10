package com.requirementanalyzer.backend.document;

import java.io.IOException;
import java.util.UUID;

public interface DocumentStorage {
    String store(UUID projectId, String originalFileName, byte[] content) throws IOException;

    byte[] load(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
