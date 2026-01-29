package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.service.PriceService;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import com.pangility.schwab.api.client.marketdata.model.quotes.equity.QuoteEquity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(PriceController.class)
public class PriceControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private PriceService priceService;

  @Test
  void testGetPrice() {
    QuoteResponse.EquityResponse quoteResponse = new QuoteResponse.EquityResponse();
    quoteResponse.setSymbol("foo");
    QuoteEquity quote = new QuoteEquity();
    quote.setMark(BigDecimal.TEN);
    quoteResponse.setQuote(quote);

    when(priceService.getQuoteResponse(anyString()))
        .thenReturn(Mono.just(quoteResponse));

    webTestClient.get()
        .uri("/api/v1/price?symbol=foo")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.symbol").isEqualTo("foo")
        .jsonPath("$.quote.mark").isEqualTo(BigDecimal.TEN);

    verify(priceService, times(1)).getQuoteResponse(anyString());
  }
}
