package com.aituan.tradefulfillment.trade.client;

public interface DistanceClient {

  double distanceKm(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude);

  DistanceEstimate estimate(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude);

  record DistanceEstimate(String distanceText, String estimatedTimeText) {}
}
