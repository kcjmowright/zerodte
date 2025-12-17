package com.kcjmowright.zerodte.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kcjmowright.zerodte.model.InstrumentType;
import lombok.Data;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "position")
@Data
@ToString
public class PositionEntity {

  @Id
  @GeneratedValue( strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "symbol")
  private String symbol;

  @Column(name = "type")
  private InstrumentType type;

  @Column(name = "quantity")
  private BigDecimal quantity;

  @Column(name = "purchaseprice")
  private BigDecimal purchasePrice;

  @Column(name = "sellprice")
  private BigDecimal sellPrice;

  @Column(name = "created")
  private LocalDateTime created;

  @Column(name = "closed")
  private LocalDateTime closed;
}
