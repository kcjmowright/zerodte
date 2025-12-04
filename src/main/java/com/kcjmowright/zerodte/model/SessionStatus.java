package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SessionStatus {
  private String username;
  private LocalDateTime expiration;
  private Boolean expired;
}
