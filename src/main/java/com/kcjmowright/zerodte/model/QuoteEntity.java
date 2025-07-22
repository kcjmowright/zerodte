package com.kcjmowright.zerodte.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote")
@Data
@ToString
public class QuoteEntity {

  @Id
  @GeneratedValue( strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "symbol")
  private String symbol;

  @Column(name = "mark")
  private BigDecimal mark;

  @Column(name = "high")
  private BigDecimal high;

  @Column(name = "low")
  private BigDecimal low;

  @Column(name = "open")
  private BigDecimal open;

  @Column(name = "close")
  private BigDecimal close;

  @Column(name = "created")
  private LocalDateTime created;
}
