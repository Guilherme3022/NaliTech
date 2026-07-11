package com.nalitech.modules.layout.exporter;

public record ExportedFile(String filename, String contentType, byte[] content) {
}
