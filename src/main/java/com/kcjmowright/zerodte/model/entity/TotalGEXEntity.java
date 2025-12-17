package com.kcjmowright.zerodte.model.entity;

import com.kcjmowright.zerodte.model.TotalGEX;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "totalgex")
@Setter
@Getter
public class TotalGEXEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "symbol")
  private String symbol;

  @Column(name = "created")
  private LocalDateTime created;

  @Column(name = "data", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private TotalGEX data;

}
