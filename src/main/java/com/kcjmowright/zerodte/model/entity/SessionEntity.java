package com.kcjmowright.zerodte.model.entity;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session")
@Data
@ToString
public class SessionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "username")
  private String username;

  @Column(name = "token")
  private String token;

  @Column(name = "refreshtoken")
  private String refreshToken;

  @Column(name = "created")
  private LocalDateTime created;

  @Column(name = "lastupdated")
  private LocalDateTime lastUpdated;

  @Column(name = "refreshexpiration")
  private LocalDateTime refreshExpiration;

  @Column(name = "accessexpiration")
  private LocalDateTime accessExpiration;
}
