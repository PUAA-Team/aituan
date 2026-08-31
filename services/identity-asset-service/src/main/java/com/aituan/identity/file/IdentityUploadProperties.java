package com.aituan.identity.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aituan.upload")
public class IdentityUploadProperties {
  private String rootDir = System.getProperty("user.home") + "/.aituan/uploads";
  private String publicPrefix = "/api/common/files";
  private long maxSizeBytes = 5 * 1024 * 1024;

  public String getRootDir() {
    return rootDir;
  }

  public void setRootDir(String rootDir) {
    this.rootDir = rootDir;
  }

  public String getPublicPrefix() {
    return publicPrefix;
  }

  public void setPublicPrefix(String publicPrefix) {
    this.publicPrefix = publicPrefix;
  }

  public long getMaxSizeBytes() {
    return maxSizeBytes;
  }

  public void setMaxSizeBytes(long maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
  }
}
