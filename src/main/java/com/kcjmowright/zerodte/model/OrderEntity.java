package com.kcjmowright.zerodte.model;

import java.math.BigDecimal;
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
@Table(name = "orders")
@Data
@ToString
public class OrderEntity {

  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "orderid")
  private Long orderId;

  @Column(name = "symbol")
  private String symbol;

  @Column(name = "quantity")
  private BigDecimal quantity;

  @Column(name = "type")
  private InstrumentType type;

  @Column(name = "created")
  private LocalDateTime created;

  @Column(name = "filled")
  private LocalDateTime filled;
}
