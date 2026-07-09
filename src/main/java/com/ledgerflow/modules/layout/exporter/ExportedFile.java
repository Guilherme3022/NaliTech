package com.ledgerflow.modules.layout.exporter;

public record ExportedFile(String filename, String contentType, byte[] content) {
}
