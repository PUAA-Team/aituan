package com.aituan.engagementplatform.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aituan.upload")
public class ImageStorageProperties {
  private String strategy = "local";
  private String rootDir = System.getProperty("user.home") + "/.aituan/uploads";
  private String publicPrefix = "/api/common/files";
  private long maxSizeBytes = 5242880;
  private final Lskypro lskypro = new Lskypro();

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

  public long getMaxSizeBytes() {
    return maxSizeBytes;
  }

  public void setMaxSizeBytes(long maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
  }

  public Lskypro getLskypro() {
    return lskypro;
  }

  public static class Lskypro {
    private String apiUrl = "https://p.2b.gs/api/v1";
    private String token = "";
    private String email = "";
    private String password = "";
    private String tokenUrl = "";
    private String strategyId = "";
    private String albumId = "";
    private Integer permission = 1;

    public String getApiUrl() {
      return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
      this.apiUrl = apiUrl;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getTokenUrl() {
      return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
      this.tokenUrl = tokenUrl;
    }

    public String getStrategyId() {
      return strategyId;
    }

    public void setStrategyId(String strategyId) {
      this.strategyId = strategyId;
    }

    public String getAlbumId() {
      return albumId;
    }

    public void setAlbumId(String albumId) {
      this.albumId = albumId;
    }

    public Integer getPermission() {
      return permission;
    }

    public void setPermission(Integer permission) {
      this.permission = permission;
    }
  }
}

