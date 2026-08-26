package com.aituan.discovery;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/location")
@Validated
class LocationController {
  private final MapDistanceService mapDistanceService;

  LocationController(MapDistanceService mapDistanceService) {
    this.mapDistanceService = mapDistanceService;
  }

  @GetMapping("/reverse-geocode")
  ApiResponse<MapDistanceService.ReverseGeocodeResult> reverseGeocode(
      @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
      @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude) {
    return ApiResponse.ok(mapDistanceService.reverseGeocode(longitude, latitude));
  }
}
