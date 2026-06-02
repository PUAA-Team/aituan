package com.aituan.common.file;

import java.time.LocalDateTime;

public record FileAssetView(
    Long id,
    String bizType,
    String originalName,
    String publicUrl,
    String mimeType,
    Long sizeBytes,
    LocalDateTime createdAt) {}
