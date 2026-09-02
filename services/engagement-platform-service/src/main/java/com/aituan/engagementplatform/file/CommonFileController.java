package com.aituan.engagementplatform.file;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common/files")
@Validated
class CommonFileController {
  private final FileStorageService fileStorageService;

  CommonFileController(FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<FileAssetView> upload(@RequestParam("file") MultipartFile file, @RequestParam @NotBlank String bizType) {
    return ApiResponse.ok(fileStorageService.save(file, bizType));
  }

  @GetMapping("/{bizType}/{fileName}")
  ResponseEntity<Resource> file(@PathVariable String bizType, @PathVariable String fileName) throws IOException {
    Resource resource = fileStorageService.load(bizType + "/" + fileName);
    MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
        .body(resource);
  }
}
