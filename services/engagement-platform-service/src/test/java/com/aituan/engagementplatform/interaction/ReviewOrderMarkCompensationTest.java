package com.aituan.engagementplatform.interaction;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.engagementplatform.client.PlatformRemoteClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewOrderMarkCompensationTest {
  @Test
  void retriesWithStableReviewIdAndRecordsSuccessOrFailure() {
    InteractionRepository repository = mock(InteractionRepository.class);
    PlatformRemoteClient remoteClient = mock(PlatformRemoteClient.class);
    when(repository.claimPendingOrderMarks(10, 50, 300)).thenReturn(List.of(
        new InteractionRepository.PendingOrderMark(101, 201),
        new InteractionRepository.PendingOrderMark(102, 202)));
    doThrow(new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "trade unavailable"))
        .when(remoteClient).markOrderReviewed(202, 102);

    new ReviewOrderMarkCompensation(repository, remoteClient, 50, 10, 300).retryPending();

    verify(remoteClient).markOrderReviewed(201, 101);
    verify(repository).markOrderSyncSucceeded(101);
    verify(remoteClient).markOrderReviewed(202, 102);
    verify(repository).markOrderSyncFailed(102, "trade unavailable");
  }
}
