package com.kcjmowright.zerodte.model;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;

public record ExpirationDateStrike(LocalDateTime expirationDate, BigDecimal strikePrice)
    implements Comparable<ExpirationDateStrike> {

  @Override
  public int compareTo(@NonNull ExpirationDateStrike other) {
    return Comparator
        .comparing(ExpirationDateStrike::expirationDate)
        .thenComparing(ExpirationDateStrike::strikePrice)
        .compare(this, other);
  }

}
