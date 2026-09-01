package com.aituan.engagementplatform.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class StationMessageClientTest {
  @Test
  void keepsBusinessResultAndRecordsTraceWhenNotificationFails() {
    PlatformRemoteClient remoteClient = mock(PlatformRemoteClient.class);
    NotificationFailureRecorder recorder = mock(NotificationFailureRecorder.class);
    doThrow(new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "identity unavailable"))
        .when(remoteClient).publishMessage(any(), anyString());

    new StationMessageClient(remoteClient, recorder)
        .complaint(7, "投诉已受理", "处理中", 11L, 13L, "处理中");

    verify(recorder).record(
        org.mockito.ArgumentMatchers.eq("complaint"),
        org.mockito.ArgumentMatchers.eq(13L),
        anyString(),
        org.mockito.ArgumentMatchers.eq("identity unavailable"));
  }
}
