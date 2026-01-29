package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.IronCondorContracts;
import com.pangility.schwab.api.client.accountsandtrading.SchwabAccountsAndTradingApiClient;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.AssetType;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.EquityInstrument;
import com.pangility.schwab.api.client.accountsandtrading.model.order.ComplexOrderStrategyType;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Order;
import com.pangility.schwab.api.client.accountsandtrading.model.order.OrderLegCollection;
import com.pangility.schwab.api.client.accountsandtrading.model.order.OrderRequest;
import com.pangility.schwab.api.client.accountsandtrading.model.order.OrderType;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Session;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

  private final SchwabApiClientTokenService tokenService;
  private final SchwabAccountsAndTradingApiClient accountsAndTradingClient;

  public Flux<Order> fetchOrders(ZonedDateTime from, ZonedDateTime to) {
    OrderRequest orderRequest = OrderRequest.builder()
        .withFromEnteredTime(from)
        .withToEnteredTime(to)
        .build();
    return accountsAndTradingClient
        .fetchOrdersToFlux(tokenService.getUserId(), tokenService.getEncryptedAccountHash(), orderRequest)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
  }

  /**
   * Build an Iron Condor option order from the given contracts.
   * @param ironCondorContracts The contracts used in an Iron Condor.
   * @return an {@link Order} built from the given contracts.
   */
  public Mono<Order> buildIronCondorOrder(Mono<IronCondorContracts> ironCondorContracts, BigDecimal quantity) {
    return ironCondorContracts.map(ironCondor -> {
      OptionContract shortPut = ironCondor.shortPut();
      OptionContract longPut = ironCondor.longPut();
      OptionContract shortCall = ironCondor.shortCall();
      OptionContract longCall = ironCondor.longCall();

      List<OrderLegCollection> orderLegCollections = List.of(
          createOrderLegCollection(shortPut.getSymbol(), quantity.negate(), OrderLegCollection.Instruction.SELL_TO_OPEN),
          createOrderLegCollection(longPut.getSymbol(), quantity, OrderLegCollection.Instruction.BUY_TO_OPEN),
          createOrderLegCollection(shortCall.getSymbol(), quantity.negate(), OrderLegCollection.Instruction.SELL_TO_OPEN),
          createOrderLegCollection(longCall.getSymbol(), quantity, OrderLegCollection.Instruction.BUY_TO_OPEN));

      BigDecimal midShortPut = shortPut.getAskPrice()
          .add(shortPut.getBidPrice())
          .divide(BigDecimal.TWO, MathContext.DECIMAL64).setScale(3, RoundingMode.HALF_UP);
      BigDecimal midLongPut = longPut.getAskPrice()
          .add(longPut.getBidPrice())
          .divide(BigDecimal.TWO, MathContext.DECIMAL64).setScale(3, RoundingMode.HALF_UP);
      BigDecimal midShortCall = shortCall.getAskPrice()
          .add(shortCall.getBidPrice())
          .divide(BigDecimal.TWO, MathContext.DECIMAL64).setScale(3, RoundingMode.HALF_UP);
      BigDecimal midLongCall = longCall.getAskPrice()
          .add(longCall.getBidPrice())
          .divide(BigDecimal.TWO, MathContext.DECIMAL64).setScale(3, RoundingMode.HALF_UP);

      Order order = new Order();
      // order.setRequestedDestination(RequestedDestination.AUTO);
      order.setSession(Session.NORMAL);
      order.setDuration(com.pangility.schwab.api.client.accountsandtrading.model.order.Duration.DAY);
      order.setOrderType(OrderType.LIMIT);
      order.setQuantity(quantity.negate());
      order.setPrice(midLongPut.subtract(midShortPut).add(midLongCall).subtract(midShortCall));
      order.setComplexOrderStrategyType(ComplexOrderStrategyType.IRON_CONDOR);
      order.setOrderLegCollection(orderLegCollections);
      return order;
    });
  }


  /**
   * Create an order leg.
   *
   * @param symbol the ticker symbol of interest.
   * @param quantity the quantity
   * @param instruction Buy to open, buy to close, sell to open, sell to close.
   * @return an OrderLegCollection
   */
  public OrderLegCollection createOrderLegCollection(
      String symbol,
      BigDecimal quantity,
      OrderLegCollection.Instruction instruction) {

    EquityInstrument instrument = new EquityInstrument();
    instrument.setAssetType(AssetType.OPTION);
    instrument.setSymbol(symbol);
    OrderLegCollection orderLegCollection = new OrderLegCollection();
    orderLegCollection.setInstruction(instruction);
    orderLegCollection.setQuantity(quantity);
    orderLegCollection.setInstrument(instrument);
    return orderLegCollection;
  }

  /**
   * Fetch an {@link Order} by ID.
   * @param orderId the {@link Order} ID.
   * @return the {@link Order}
   */
  public Mono<Order> fetchOrder(Long orderId) {
    return accountsAndTradingClient
        .fetchOrderToMono(tokenService.getUserId(), tokenService.getEncryptedAccountHash(), orderId)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
  }


  /**
   * Place a live order on the broker platform.
   * @param order {@link Order}.
   * @return the broker response.
   */
  Mono<String> placeOrder(Order order) {
    return accountsAndTradingClient
        .placeOrder(tokenService.getUserId(), tokenService.getEncryptedAccountHash(), order)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
  }
}
