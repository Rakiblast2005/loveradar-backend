package com.loveradar.controller;

import com.loveradar.dto.location.LocationUpdateRequest;
import com.loveradar.dto.location.LocationUpdateResponse;
import com.loveradar.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public ResponseEntity<LocationUpdateResponse> update(@Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(request));
    }
}
