package com.kcjmowright.zerodte.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GEXFeatures {
  private BigDecimal distanceToCallWall;
  private BigDecimal distanceToPutWall;
  private BigDecimal distanceToFlipPoint;
  private BigDecimal callPutGEXRatio;
  private BigDecimal netGEX;
  private BigDecimal gexSkew;
  private BigDecimal concentrationIndex;
  private BigDecimal relativePosition; // position between put/call walls
  private Integer minutesToExpiry;
  private BigDecimal priceVelocity; // recent price change
}
