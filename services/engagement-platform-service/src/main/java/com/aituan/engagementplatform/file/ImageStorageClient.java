package com.aituan.engagementplatform.file;

import org.springframework.web.multipart.MultipartFile;

interface ImageStorageClient {
  StoredImage save(MultipartFile file, String bizType, String filename);
}
