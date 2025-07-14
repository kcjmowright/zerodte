package com.kcjmowright.zerodte.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options symbols are broken down as:
 * Underlying Symbol (6 characters including spaces) | Expiration (6 characters) | Call/Put (1 character) | Strike Price (5+3=8
 * characters)
 * Option Symbol: XYZ 210115C00050000
 */
@NoArgsConstructor
@Data
public class TickerSymbol {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
  private static final Pattern regex = Pattern.compile("^([A-Za-z]{1,6})\\s*?(\\d{6})([CP])(\\d{6,8})$");
  private static final BigDecimal THOUSAND = new BigDecimal("1000.00", MathContext.DECIMAL32);
  private String symbol;
  private String underlyingSymbol;
  private InstrumentType type;
  private LocalDateTime expirationDate;
  private BigDecimal strikePrice;

  public static TickerSymbol fromString(String symbol) {
    if (symbol == null || symbol.isEmpty()) {
      return null;
    }
    TickerSymbol tickerSymbol = new TickerSymbol();
    tickerSymbol.setSymbol(symbol);
    if (regex.matcher(symbol).matches()) {
      Matcher parts = regex.matcher(symbol);
      if (parts.find()) {
        tickerSymbol.setUnderlyingSymbol(parts.group(1));
        tickerSymbol.setExpirationDate(LocalDateTime.of(LocalDate.parse(parts.group(2), DATE_FORMATTER),
            LocalTime.of(0, 0)));
        tickerSymbol.setType(Objects.equals("C", parts.group(3)) ? InstrumentType.CALL : InstrumentType.PUT);
        var strikePrice = new BigDecimal(parts.group(4))
            .divide(THOUSAND, MathContext.DECIMAL32)
            .setScale(3, RoundingMode.HALF_UP);
        tickerSymbol.setStrikePrice(strikePrice);
      }
    } else {
      tickerSymbol.setType(InstrumentType.STOCK);
    }
    return tickerSymbol;
  }
}
