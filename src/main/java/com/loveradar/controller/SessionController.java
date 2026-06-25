package com.loveradar.controller;

import com.loveradar.dto.session.CreateSessionRequest;
import com.loveradar.dto.session.JoinSessionRequest;
import com.loveradar.dto.session.SessionResponse;
import com.loveradar.dto.session.UpdateRadiusRequest;
import com.loveradar.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/create")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(request));
    }

    @PostMapping("/join")
    public ResponseEntity<SessionResponse> join(@Valid @RequestBody JoinSessionRequest request) {
        return ResponseEntity.ok(sessionService.joinSession(request.getSessionCode()));
    }

    @PostMapping("/end")
    public ResponseEntity<SessionResponse> end() {
        return ResponseEntity.ok(sessionService.endSession());
    }

    @GetMapping("/active")
    public ResponseEntity<SessionResponse> active() {
        return sessionService.getActiveSession()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/radius")
    public ResponseEntity<SessionResponse> updateRadius(@Valid @RequestBody UpdateRadiusRequest request) {
        return ResponseEntity.ok(sessionService.updateRadius(request));
    }

    @PostMapping("/emergency")
    public ResponseEntity<SessionResponse> setEmergency(@RequestParam(defaultValue = "true") boolean hidden) {
        return ResponseEntity.ok(sessionService.setEmergencyHidden(hidden));
    }
}
