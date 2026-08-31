package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.DistanceClient;
import org.springframework.stereotype.Component;

@Component
public class LocalDistanceClient implements DistanceClient {
  private static final double EARTH_RADIUS_KM = 6371.0;

  @Override
  public double distanceKm(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
    double lat1 = Math.toRadians(fromLatitude);
    double lat2 = Math.toRadians(toLatitude);
    double deltaLat = Math.toRadians(toLatitude - fromLatitude);
    double deltaLon = Math.toRadians(toLongitude - fromLongitude);
    double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
    return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  @Override
  public DistanceEstimate estimate(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
    double distance = distanceKm(fromLatitude, fromLongitude, toLatitude, toLongitude);
    int minutes = Math.max(15, (int) Math.ceil(distance / 0.35));
    return new DistanceEstimate(String.format("%.2fkm", distance), minutes + "分钟");
  }
}
