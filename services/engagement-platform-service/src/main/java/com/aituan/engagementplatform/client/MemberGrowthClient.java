package com.aituan.engagementplatform.client;

import org.springframework.stereotype.Service;

@Service
public class MemberGrowthClient {
  private final PlatformRemoteClient remoteClient;

  MemberGrowthClient(PlatformRemoteClient remoteClient) {
    this.remoteClient = remoteClient;
  }

  public void addReviewGrowth(long userId, long reviewId) {
    remoteClient.addReviewGrowth(userId, reviewId);
  }
}
