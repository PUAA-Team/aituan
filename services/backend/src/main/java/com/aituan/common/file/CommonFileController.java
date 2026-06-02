package com.aituan.common.file;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(resource.contentLength() > 0 ? "application/octet-stream" : "application/octet-stream"))
        .body(resource);
  }
}
