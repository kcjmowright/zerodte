package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.SessionEntity;
import com.kcjmowright.zerodte.model.SessionStatus;
import com.kcjmowright.zerodte.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1")
public class SessionController {
  private final SessionService sessionService;

  @GetMapping("/session/status")
  public ResponseEntity<SessionStatus> getSessionStatus(@CookieValue(value = "zun", defaultValue = "") String username) {
    if (username == null || username.isEmpty()) {
      SessionStatus status = new SessionStatus(null, null, true);
      return ResponseEntity.ok().body(status);
    }
    SessionEntity session = sessionService.findByUsername(username);
    SessionStatus status = new SessionStatus(session.getUsername(), session.getRefreshExpiration(),
        session.getRefreshExpiration().isBefore(LocalDateTime.now()));
    if (status.getExpired() == Boolean.FALSE) {
      ResponseCookie cookie = ResponseCookie.from("zun", session.getUsername())
          .httpOnly(true)
          .secure(true)
          .path("/")
          .maxAge(Duration.between(LocalDateTime.now(), session.getRefreshExpiration()))
          .sameSite("Lax")
          .build();
      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, cookie.toString())
          .body(status);
    }
    return ResponseEntity.ok().body(status);
  }

  @DeleteMapping("/session")
  public Mono<Void> deleteSession(@CookieValue("zun") String username) {
    if (!(username == null || username.isEmpty())) {
      sessionService.deleteByUsername(username);
    }
    return Mono.empty();
  }
}
