package com.aituan.identity.file;

import java.time.LocalDateTime;

public record FileAssetView(
    Long id,
    String bizType,
    String originalName,
    String publicUrl,
    String mimeType,
    long sizeBytes,
    LocalDateTime createdAt) {}
