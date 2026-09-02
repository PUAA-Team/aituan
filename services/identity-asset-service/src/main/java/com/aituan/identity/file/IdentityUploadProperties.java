package com.aituan.identity.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aituan.upload")
public class IdentityUploadProperties {
  private String strategy = "platform";
  private String rootDir = System.getProperty("user.home") + "/.aituan/uploads";
  private String publicPrefix = "/api/common/files";
  private String platformBaseUrl = "http://engagement-platform-service:8084";
  private long maxSizeBytes = 5 * 1024 * 1024;

  public String getStrategy() {
    return strategy;
  }

  public void setStrategy(String strategy) {
    this.strategy = strategy;
  }

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

  public String getPlatformBaseUrl() {
    return platformBaseUrl;
  }

  public void setPlatformBaseUrl(String platformBaseUrl) {
    this.platformBaseUrl = platformBaseUrl;
  }

  public long getMaxSizeBytes() {
    return maxSizeBytes;
  }

  public void setMaxSizeBytes(long maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
  }
}
