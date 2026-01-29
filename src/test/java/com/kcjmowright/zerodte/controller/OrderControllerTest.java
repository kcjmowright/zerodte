package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.IronCondorContracts;
import com.kcjmowright.zerodte.service.OrderService;
import com.kcjmowright.zerodte.service.PriceService;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.AssetType;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.OptionInstrument;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.PutCall;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Order;
import com.pangility.schwab.api.client.accountsandtrading.model.order.OrderLegCollection;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
public class OrderControllerTest {

  @MockitoBean
  private OrderService orderService;

  @MockitoBean
  private PriceService priceService;

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void testGetOrders() {
    final ZonedDateTime enteredTime = ZonedDateTime.now();

    final Order order1 = new Order();
    order1.setAccountNumber(1234L);
    order1.setOrderId(1L);
    order1.setCancelable(false);
    order1.setEnteredTime(enteredTime);
    order1.setQuantity(BigDecimal.TWO);
    order1.setPrice(BigDecimal.TEN);

    final Order order2 = new Order();
    order2.setAccountNumber(1234L);
    order2.setOrderId(2L);
    order2.setCancelable(true);
    order2.setEnteredTime(enteredTime);
    order2.setQuantity(BigDecimal.ONE);
    order2.setPrice(BigDecimal.ONE);

    when(orderService.fetchOrders(any(), any()))
        .thenReturn(Flux.just(order1, order2));

    webTestClient.get()
        .uri("/api/v1/orders")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
//        .consumeWith(response ->
//            System.out.println("Response Body: " + new String(response.getResponseBody()))
//        )
        .jsonPath("$[0].accountNumber").isEqualTo(1234L)
        .jsonPath("$[0].orderId").isEqualTo(1L)
        .jsonPath("$[0].cancelable").isEqualTo(false)
        .jsonPath("$[0].enteredTime").isEqualTo(enteredTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .jsonPath("$[0].quantity").isEqualTo(BigDecimal.TWO)
        .jsonPath("$[0].price").isEqualTo(BigDecimal.TEN)
        .jsonPath("$[1].accountNumber").isEqualTo(1234L)
        .jsonPath("$[1].orderId").isEqualTo(2L)
        .jsonPath("$[1].cancelable").isEqualTo(true)
        .jsonPath("$[1].enteredTime").isEqualTo(enteredTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .jsonPath("$[1].quantity").isEqualTo(BigDecimal.ONE)
        .jsonPath("$[1].price").isEqualTo(BigDecimal.ONE);

    verify(orderService, times(1)).fetchOrders(any(), any());
  }

  @Test
  void testGetZeroDTEIronCondorOrder() {
    final ZonedDateTime enteredTime = ZonedDateTime.now();
    final String symbol = "foo";
    final long accountNo = 1234L;

    OrderLegCollection leg1 = new OrderLegCollection();
    OptionInstrument instrument1 = new OptionInstrument();
    instrument1.setAssetType(AssetType.OPTION);
    instrument1.setSymbol(symbol);
    instrument1.setInstrumentId(1L);
    instrument1.setPutCall(PutCall.PUT);
    leg1.setInstrument(instrument1);
    leg1.setQuantity(BigDecimal.ONE);
    leg1.setLegId(1L);
    leg1.setOrderLegType(OrderLegCollection.OrderLegType.OPTION);

    OrderLegCollection leg2 = new OrderLegCollection();
    OptionInstrument instrument2 = new OptionInstrument();
    instrument2.setAssetType(AssetType.OPTION);
    instrument2.setSymbol(symbol);
    instrument2.setInstrumentId(2L);
    instrument2.setPutCall(PutCall.PUT);
    leg2.setInstrument(instrument2);
    leg2.setQuantity(BigDecimal.ONE);
    leg2.setLegId(2L);
    leg2.setOrderLegType(OrderLegCollection.OrderLegType.OPTION);

    OrderLegCollection leg3 = new OrderLegCollection();
    OptionInstrument instrument3 = new OptionInstrument();
    instrument3.setAssetType(AssetType.OPTION);
    instrument3.setSymbol(symbol);
    instrument3.setInstrumentId(3L);
    instrument3.setPutCall(PutCall.CALL);
    leg3.setInstrument(instrument3);
    leg3.setQuantity(BigDecimal.ONE);
    leg3.setLegId(3L);
    leg2.setOrderLegType(OrderLegCollection.OrderLegType.OPTION);

    OrderLegCollection leg4 = new OrderLegCollection();
    OptionInstrument instrument4 = new OptionInstrument();
    instrument4.setAssetType(AssetType.OPTION);
    instrument4.setSymbol(symbol);
    instrument4.setInstrumentId(4L);
    instrument4.setPutCall(PutCall.CALL);
    leg4.setInstrument(instrument4);
    leg4.setQuantity(BigDecimal.ONE);
    leg4.setLegId(4L);
    leg4.setOrderLegType(OrderLegCollection.OrderLegType.OPTION);

    final Order order = new Order();
    order.setAccountNumber(accountNo);
    order.setOrderId(1L);
    order.setCancelable(false);
    order.setEnteredTime(enteredTime);
    order.setQuantity(BigDecimal.TWO);
    order.setPrice(BigDecimal.TEN);
    order.setOrderLegCollection(List.of(leg1, leg2, leg3, leg4));

    OptionContract longPut = new OptionContract();
    OptionContract shortPut = new OptionContract();
    OptionContract shortCall = new OptionContract();
    OptionContract longCall = new OptionContract();
    IronCondorContracts ironCondorContracts = new IronCondorContracts(longCall, shortCall, longPut, shortPut);

    when(orderService.buildIronCondorOrder(any(), eq(BigDecimal.valueOf(1.0))))
        .thenReturn(Mono.just(order));
    when(priceService.findIronCondor(eq(symbol), any(), any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(ironCondorContracts));

    webTestClient.get()
        .uri("/api/v1/option/zero-dte-iron-condor/order?symbol=foo")
        //.accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .consumeWith(response ->
            System.out.println("Response Body: " + new String(response.getResponseBody()))
        )
        .jsonPath("$.accountNumber").isEqualTo(accountNo)
        .jsonPath("$.orderId").isEqualTo(1L)
        .jsonPath("$.cancelable").isEqualTo(false)
        .jsonPath("$.enteredTime").isEqualTo(enteredTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .jsonPath("$.quantity").isEqualTo(BigDecimal.TWO)
        .jsonPath("$.price").isEqualTo(BigDecimal.TEN)

        .jsonPath("$.orderLegCollection[0].instrument.assetType").isEqualTo("OPTION")
        .jsonPath("$.orderLegCollection[0].instrument.putCall").isEqualTo("PUT")
        .jsonPath("$.orderLegCollection[0].instrument.symbol").isEqualTo(symbol)
        .jsonPath("$.orderLegCollection[0].instrument.instrumentId").isEqualTo(1L)
        .jsonPath("$.orderLegCollection[0].quantity").isEqualTo(1)
        .jsonPath("$.orderLegCollection[0].legId").isEqualTo(1L)

        .jsonPath("$.orderLegCollection[1].instrument.assetType").isEqualTo("OPTION")
        .jsonPath("$.orderLegCollection[1].instrument.putCall").isEqualTo("PUT")
        .jsonPath("$.orderLegCollection[1].instrument.symbol").isEqualTo(symbol)
        .jsonPath("$.orderLegCollection[1].instrument.instrumentId").isEqualTo(2L)
        .jsonPath("$.orderLegCollection[1].quantity").isEqualTo(1)
        .jsonPath("$.orderLegCollection[1].legId").isEqualTo(2L)

        .jsonPath("$.orderLegCollection[2].instrument.assetType").isEqualTo("OPTION")
        .jsonPath("$.orderLegCollection[2].instrument.putCall").isEqualTo("CALL")
        .jsonPath("$.orderLegCollection[2].instrument.symbol").isEqualTo(symbol)
        .jsonPath("$.orderLegCollection[2].instrument.instrumentId").isEqualTo(3L)
        .jsonPath("$.orderLegCollection[2].quantity").isEqualTo(1)
        .jsonPath("$.orderLegCollection[2].legId").isEqualTo(3L)

        .jsonPath("$.orderLegCollection[3].instrument.assetType").isEqualTo("OPTION")
        .jsonPath("$.orderLegCollection[3].instrument.putCall").isEqualTo("CALL")
        .jsonPath("$.orderLegCollection[3].instrument.symbol").isEqualTo(symbol)
        .jsonPath("$.orderLegCollection[3].instrument.instrumentId").isEqualTo(4L)
        .jsonPath("$.orderLegCollection[3].quantity").isEqualTo(1)
        .jsonPath("$.orderLegCollection[3].legId").isEqualTo(4L);


    verify(orderService, times(1)).buildIronCondorOrder(any(), eq(BigDecimal.valueOf(1.0)));
    verify(priceService, times(1)).findIronCondor(eq(symbol), any(), any(), any(), any(), any(), any());
  }
}
