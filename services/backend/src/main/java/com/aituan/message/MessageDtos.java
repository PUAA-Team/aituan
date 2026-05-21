package com.aituan.message;

import java.time.LocalDateTime;

record MessageView(Long id, String type, String title, String content, String badgeText, boolean unread, Long relatedOrderId, LocalDateTime createdAt) {}
