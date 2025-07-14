package com.kcjmowright.zerodte.service;

import static java.util.function.Predicate.not;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.kcjmowright.zerodte.model.*;
import com.kcjmowright.zerodte.repository.OrderRepository;
import com.kcjmowright.zerodte.repository.PositionRepository;
import com.kcjmowright.zerodte.repository.QuoteRepository;
import com.kcjmowright.zerodte.repository.SessionRepository;
import com.pangility.schwab.api.client.accountsandtrading.SchwabAccountsAndTradingApiClient;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Account;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Position;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.AssetType;
import com.pangility.schwab.api.client.accountsandtrading.model.instrument.EquityInstrument;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Duration;
import com.pangility.schwab.api.client.accountsandtrading.model.order.*;
import com.pangility.schwab.api.client.common.EnableSchwabApi;
import com.pangility.schwab.api.client.marketdata.SchwabMarketDataApiClient;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainRequest;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionChainResponse;
import com.pangility.schwab.api.client.marketdata.model.chains.OptionContract;
import com.pangility.schwab.api.client.marketdata.model.movers.MoversRequest;
import com.pangility.schwab.api.client.marketdata.model.movers.Screener;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import com.pangility.schwab.api.client.oauth2.SchwabAccount;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@EnableSchwabApi
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ZeroDTEAgentService {

  private static final LocalTime TRADE_TIME_OPEN = LocalTime.of(9, 0); // 8:45 AM CST
  private static final LocalTime OPENING_TRADE_TIME_END = LocalTime.of(9, 30); // 9:00 AM CST
  private static final LocalTime CLOSE_TIME = LocalTime.of(15, 0); // 3:00 PM CST
  private static final long CLOSE_TIME_MINUTES_BEFORE = 10L; // Close all remaining positions 10 minutes before close
  private static final ZoneId CST = ZoneId.of("America/Chicago");

  private final OrderRepository orderRepository;
  private final PositionRepository positionRepository;
  private final QuoteRepository quoteRepository;
  private final SchwabAccountsAndTradingApiClient accountsAndTradingClient;
  private final SchwabApiClientTokenService schwabApiClientTokenService;
  private final SchwabMarketDataApiClient marketDataClient;
  private final SessionRepository sessionRepository;

  @Value("${zerodte.agent.userId}")
  private String userId;

  @Value("${zerodte.agent.accountNumber}")
  private String accountNumber;

  @Value("${zerodte.agent.symbol:QQQ}")
  private String symbol;

  @Value("${zerodte.agent.quantity:-1.0}")
  private BigDecimal quantity;

  @Value("${zerodte.agent.put.shortDelta:-0.40}")
  private BigDecimal putShortDelta;

  @Value("${zerodte.agent.put.longDelta:-0.10}")
  private BigDecimal putLongDelta;

  @Value("${zerodte.agent.call.shortDelta:0.40}")
  private BigDecimal callShortDelta;

  @Value("${zerodte.agent.call.longDelta:0.10}")
  private BigDecimal callLongDelta;

  @Value("${zerodte.agent.profitTargetPercent:0.9}")
  private BigDecimal profitTargetPercent;

  @Value("${zerodte.agent.lossLimitPercent:0.7}")
  private BigDecimal lossLimitPercent;

  @Value("${zerodte.agent.simulate}")
  private Boolean simulated;

  private final AtomicReference<Long> openOrderId = new AtomicReference<>();

  private final AtomicReference<Set<TickerSymbol>> openPositionIds = new AtomicReference<>(new HashSet<>());

  private final AtomicReference<Set<TickerSymbol>> closedPositionIds = new AtomicReference<>(new HashSet<>());

  private String encryptedAccountHash;

  /**
   * Execute the Zero Date Iron Condor Strategy.
   */
  @Scheduled(cron = "0 * 8-15 * * MON-FRI") // Every minute on weekdays
  public void executeStrategy() {
    log.info("Executing strategy.  Open order id: {}, Open Positions: {}, Closed Positions: {}",
        openOrderId.get(), openPositionIds.get(), closedPositionIds.get());
    ZonedDateTime now = ZonedDateTime.now(CST);
    if (now.getDayOfWeek().getValue() > 5) {
      return; // Skip weekends
    }
    LocalTime nowTime = now.toLocalTime();
    final LocalDate today = LocalDate.now();
    if (nowTime.isAfter(TRADE_TIME_OPEN) && nowTime.isBefore(CLOSE_TIME)) {
      if (nowTime.isBefore(OPENING_TRADE_TIME_END) && closedPositionIds.get().isEmpty() && openPositionIds.get().isEmpty()) {
        if (openOrderId.get() == null) {
          submitIronCondorOrder(this.symbol, today, today);
        } else {
          monitorOpenOrder();
        }
      } else if (!openPositionIds.get().isEmpty()) {
        monitorAndClosePositions();
      }
    }
  }

  /**
   * Get a {@link QuoteResponse} for the given symbol.
   * @param symbol the equity symbol.
   * @return the {@link QuoteResponse}
   */
  public Mono<QuoteResponse> getQuote(String symbol) {
    return marketDataClient.fetchQuoteToMono(symbol)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
        // .log();
  }

  /**
   * Get the option chains for the given date range.
   * @param symbol the underlying equity symbol.
   * @param fromDate the from date.
   * @param toDate the to date.
   * @return the {@link OptionChainResponse}
   */
  public Mono<OptionChainResponse> getOptionChain(String symbol, LocalDate fromDate, LocalDate toDate) {
    var request = OptionChainRequest.builder().withFromDate(fromDate).withToDate(toDate).withSymbol(symbol).build();
    return marketDataClient.fetchOptionChainToMono(request)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
        // .log();
  }

  /**
   * Get the movers for a given index symbol.
   * @param indexSymbol the {@link MoversRequest.IndexSymbol}
   * @return a {@link Screener}
   */
  public Flux<Screener> callMovers(MoversRequest.IndexSymbol indexSymbol) {
    MoversRequest moversRequest = MoversRequest.builder().withIndexSymbol(indexSymbol).build();
    return marketDataClient.fetchMoversToFlux(moversRequest);
  }

  /**
   * Find the contracts that would make up an Iron Condor for the given underlying symbol and date range.
   * @param symbol the underlying equity symbol.
   * @param fromDate the from date.
   * @param toDate the to date.
   * @return a {@link IronCondorContracts}
   */
  public Mono<IronCondorContracts> findIronCondor(String symbol, LocalDate fromDate, LocalDate toDate) {
    return getOptionChain(symbol, fromDate, toDate)
        .handle((optionChainResponse, sink) -> {
          if (optionChainResponse == null
              || optionChainResponse.getCallExpDateMap() == null
              || optionChainResponse.getCallExpDateMap().isEmpty()
              || optionChainResponse.getPutExpDateMap() == null
              || optionChainResponse.getPutExpDateMap().isEmpty()) {
            log.error("Option chain is empty or null.");
            sink.error(new RuntimeException("Option chain is empty or null."));
            return;
          }

          List<OptionContract> callContracts =
              optionChainResponse.getCallExpDateMap().values().iterator().next().values().stream()
              .flatMap(Collection::stream).toList();
          List<OptionContract> putContracts =
              optionChainResponse.getPutExpDateMap().values().iterator().next().values().stream()
              .flatMap(Collection::stream).toList();

          Optional<OptionContract> optionalShortPut =
              findOption(putContracts, this.putShortDelta, OptionContract.PutCall.PUT);
          Optional<OptionContract> optionalLongPut =
              findOption(putContracts, this.putLongDelta, OptionContract.PutCall.PUT);
          Optional<OptionContract> optionalShortCall =
              findOption(callContracts, this.callShortDelta, OptionContract.PutCall.CALL);
          Optional<OptionContract> optionalLongCall =
              findOption(callContracts, this.callLongDelta, OptionContract.PutCall.CALL);

          if (optionalShortPut.isEmpty()
              || optionalLongPut.isEmpty()
              || optionalShortCall.isEmpty()
              || optionalLongCall.isEmpty()) {
            log.error("Could not find required options.");
            sink.error(new RuntimeException("Could not find required options"));
            return;
          }

          OptionContract shortPut = optionalShortPut.get();
          OptionContract longPut = optionalLongPut.get();
          OptionContract shortCall = optionalShortCall.get();
          OptionContract longCall = optionalLongCall.get();
          sink.next(new IronCondorContracts(longCall, shortCall, longPut, shortPut));
        });
  }

  /**
   * Get the {@link QuoteResponse}s for the given equity symbols.
   * @param symbols equity symbols.
   * @return {@link QuoteResponse}s.
   */
  public Flux<QuoteResponse> getQuotes(Collection<String> symbols) {
    return Flux.fromIterable(symbols).flatMap(symbol -> Flux.from(getQuote(symbol)));
  }

  /**
   * Build an Iron Condor option order from the given contracts.
   * @param ironCondorContracts The contracts used in an Iron Condor.
   * @return an {@link Order} built from the given contracts.
   */
  public Mono<Order> buildIronCondorOrder(Mono<IronCondorContracts> ironCondorContracts) {
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
      order.setDuration(Duration.DAY);
      order.setOrderType(OrderType.LIMIT);
      order.setQuantity(quantity.negate());
      order.setPrice(midLongPut.subtract(midShortPut).add(midLongCall).subtract(midShortCall));
      order.setComplexOrderStrategyType(ComplexOrderStrategyType.IRON_CONDOR);
      order.setOrderLegCollection(orderLegCollections);
      return order;
    });
  }

  /**
   * Fetch the broker {@link Account} info.
   * @return the {@link Account}.
   */
  public Mono<Account> fetchAccount() {
    return accountsAndTradingClient.fetchAccountToMono(userId, getEncryptedAccountHash(), "positions")
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
        // .log();
  }

  /**
   * Fetch {@link Order}s entered today.
   * @return Today's {@link Order}s.
   */
  public Flux<Order> fetchOrders() {
    ZonedDateTime fromEnteredTime = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
    ZonedDateTime now = ZonedDateTime.now();
    OrderRequest orderRequest = OrderRequest.builder()
        .withFromEnteredTime(fromEnteredTime)
        .withToEnteredTime(now)
        .build();
    return accountsAndTradingClient.fetchOrdersToFlux(userId, getEncryptedAccountHash(), orderRequest)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
        // .log();
  }

  /**
   * Fetch an {@link Order} by ID.
   * @param orderId the {@link Order} ID.
   * @return the {@link Order}
   */
  public Mono<Order> fetchOrder(Long orderId) {
    return accountsAndTradingClient.fetchOrderToMono(userId, getEncryptedAccountHash(), orderId)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
        // .log();
  }

  /**
   * Place a live order on the broker platform.
   * @param order {@link Order}.
   * @return the broker response.
   */
  Mono<String> placeOrder(Order order) {
    return accountsAndTradingClient
        .placeOrder(userId, getEncryptedAccountHash(), order)
        .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)));
        // .log();
  }

  /**
   * Submit an IronCondor order.
   * @param symbol the underlying symbol.
   * @param fromDate the from date.
   * @param toDate the to date.
   */
  void submitIronCondorOrder(String symbol, LocalDate fromDate, LocalDate toDate) {
    Mono<Order> newOrder = buildIronCondorOrder(findIronCondor(symbol, fromDate, toDate));
    Mono<Order> placedOrder = simulated ?
        newOrder.map(order -> {
          order.setOrderId(new Random().nextLong());
          order.setEnteredTime(ZonedDateTime.now());
          return order;
        }) : newOrder.flatMap(order -> placeOrder(order).thenMany(fetchOrders()).next());

    placedOrder.subscribe(orderResult -> {
      openOrderId.set(orderResult.getOrderId());
      orderResult.getOrderLegCollection().forEach(leg -> {
        TickerSymbol tickerSymbol = TickerSymbol.fromString(leg.getInstrument().getSymbol());
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderId(orderResult.getOrderId());
        orderEntity.setQuantity(leg.getQuantity());
        orderEntity.setSymbol(tickerSymbol.getSymbol());
        orderEntity.setCreated(orderResult.getEnteredTime().toLocalDateTime());
        orderEntity.setType(tickerSymbol.getType());
        orderRepository.save(orderEntity);
      });
    });
  }

  /**
   * Monitor open orders for fulfillment and then fetch the resulting positions.
   */
  void monitorOpenOrder() {
    List<OrderEntity> orderEntities = orderRepository.findAllByFilledIsNull();
    List<Long> orderIds = orderEntities.stream().map(OrderEntity::getOrderId).distinct().toList();
    if (orderIds.size() != 1) {
      log.error("Expected 1 order but got {} orders:\n{}", orderIds.size(), orderEntities);
      return;
    }
    if (simulated) {
      LocalDateTime now = LocalDateTime.now();
      orderEntities.stream()
          .map(order -> {
            order.setFilled(now);
            return orderRepository.save(order);
          })
          .map(this::mapOrderToPosition)
          .forEach(positionRepository::save);
      openOrderId.set(null);
    } else {
      fetchOrder(orderIds.getFirst())
          .filter(order -> order.getStatus() == Status.FILLED)
          .blockOptional()
          .ifPresent(order -> {
            orderEntities.forEach(orderEntity -> {
              orderEntity.setFilled(order.getCloseTime().toLocalDateTime()); // @todo is this correct?
              orderRepository.save(orderEntity);
            });
            processOpenPositions(orderEntities.stream().map(OrderEntity::getSymbol).collect(Collectors.toSet()));
            openOrderId.set(null);
          });
    }
  }

  /**
   * Monitor open positions, close them if they exceed the profit or loss percentage parameters,
   * and close all positions before the market closes.
   */
  void monitorAndClosePositions() {
    log.info("Monitoring and closing positions.");
    LocalTime currentTime = LocalTime.now();
    if (currentTime.isAfter(CLOSE_TIME.minusMinutes(CLOSE_TIME_MINUTES_BEFORE))) {
      closeRemainingPositions(openPositionIds.get());
    }
    List<String> callSymbols = openPositionIds.get().stream()
        .filter(ts -> ts.getType() == InstrumentType.CALL).map(TickerSymbol::getSymbol).toList();
    if (!callSymbols.isEmpty()) {
      BigDecimal currentCallLegProfit = calculateCurrentProfitPercentage(callSymbols);
      log.info("Current call leg profit {}", currentCallLegProfit);
      if (currentCallLegProfit.compareTo(profitTargetPercent) >= 0
          || currentCallLegProfit.compareTo(lossLimitPercent.negate()) <= 0) {
        closeLegs(openPositionIds.get(), InstrumentType.CALL);
      }
    }
    List<String> putSymbols = openPositionIds.get().stream()
        .filter(ts -> ts.getType() == InstrumentType.PUT).map(TickerSymbol::getSymbol).toList();
    if (!putSymbols.isEmpty()) {
      BigDecimal currentPutLegProfit = calculateCurrentProfitPercentage(putSymbols);
      log.info("Current put leg profit {}", currentPutLegProfit);
      if (currentPutLegProfit.compareTo(profitTargetPercent) >= 0
          || currentPutLegProfit.compareTo(lossLimitPercent.negate()) <= 0) {
        closeLegs(openPositionIds.get(), InstrumentType.PUT);
      }
    }
  }

  /**
   * Fetch positions from live account and persist them.
   * @param symbols filter by set of equity symbols.
   */
  void processOpenPositions(Set<String> symbols) {
    fetchAccount()
        .flatMapMany(account -> Flux.fromIterable(account.getSecuritiesAccount().getPositions()))
        .filter(p -> symbols.contains(p.getInstrument().getSymbol()))
        .map(this::mapPositionToPositionEntity)
        .subscribe(positionRepository::save);
  }

  /**
   * Map a {@link Position} to a {@link PositionEntity}.
   * @param position {@link Position}
   * @return a @{link PositionEntity}
   */
  private PositionEntity mapPositionToPositionEntity(Position position) {
    TickerSymbol tickerSymbol = TickerSymbol.fromString(position.getInstrument().getSymbol());
    openPositionIds.get().add(tickerSymbol);
    PositionEntity positionEntity = new PositionEntity();
    positionEntity.setCreated(LocalDateTime.now());
    positionEntity.setSymbol(tickerSymbol.getSymbol());
    positionEntity.setType(tickerSymbol.getType());
    positionEntity.setPurchasePrice(position.getAveragePrice());
    var qty = position.getLongQuantity() != null && position.getLongQuantity().compareTo(BigDecimal.ZERO) != 0 ?
        position.getLongQuantity() : position.getShortQuantity();
    positionEntity.setQuantity(qty);
    return positionEntity;
  }

  /**
   *
   * @param symbols position ticker symbols of interest.
   * @return the current profit.
   */
  private BigDecimal calculateCurrentProfitPercentage(Collection<String> symbols) {
    if (symbols.isEmpty()) {
      return BigDecimal.ZERO;
    }
    Map<String, PositionEntity> positions = positionRepository.findBySymbolIn(symbols).stream()
        .collect(Collectors.toMap(PositionEntity::getSymbol, Function.identity()));
    BigDecimal purchasePrice = positions.values().stream()
        .map(position -> position.getPurchasePrice().multiply(position.getQuantity()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal currentPrice = getQuotes(symbols).toStream()
        .map(quoteResponse -> {
          PositionEntity position = positions.get(quoteResponse.getSymbol());
          BigDecimal mark = getMark(quoteResponse);
          return mark.multiply(position.getQuantity());
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal profitPercentage = purchasePrice.subtract(currentPrice).divide(purchasePrice, MathContext.DECIMAL32);
    log.info("Purchase price: {} - Current Price: {} / Purchase Price: {} = {}",
        purchasePrice, currentPrice, purchasePrice, profitPercentage);
    return profitPercentage;
  }

  /**
   * Find the "mark" or current price out of the {@link QuoteResponse}.
   * @param quoteResponse the {@link QuoteResponse}.
   * @return the current mark.
   */
  private BigDecimal getMark(QuoteResponse quoteResponse) {
    if (quoteResponse == null) {
      return null;
    }
    BigDecimal mark = switch(quoteResponse) {
      case QuoteResponse.OptionResponse or -> or.getQuote().getMark();
      case QuoteResponse.EquityResponse er -> er.getQuote().getMark();
      case QuoteResponse.IndexResponse ir -> ir.getQuote().getClosePrice();
      default -> BigDecimal.ZERO;
    };
    QuoteEntity quote = new QuoteEntity();
    quote.setSymbol(quoteResponse.getSymbol());
    quote.setMark(mark);
    quote.setCreated(LocalDateTime.now());
    quoteRepository.save(quote);
    return mark;
  }

  /**
   * Map an {@link OrderEntity} to a {@link PositionEntity}
   * @param order the {@link OrderEntity}
   * @return a {@link PositionEntity}
   */
  private PositionEntity mapOrderToPosition(OrderEntity order) {
    TickerSymbol tickerSymbol = TickerSymbol.fromString(order.getSymbol());
    PositionEntity positionEntity = new PositionEntity();
    positionEntity.setCreated(LocalDateTime.now());
    positionEntity.setSymbol(tickerSymbol.getSymbol());
    positionEntity.setType(tickerSymbol.getType());
    BigDecimal mark = getMark(getQuote(tickerSymbol.getSymbol()).block());
    positionEntity.setPurchasePrice(mark);
    positionEntity.setQuantity(order.getQuantity());
    openPositionIds.get().add(tickerSymbol);
    return positionEntity;
  }

  /**
   * Create an order leg.
   *
   * @param symbol the ticker symbol of interest.
   * @param quantity the quantity
   * @param instruction Buy to open, buy to close, sell to open, sell to close.
   * @return an OrderLegCollection
   */
  private OrderLegCollection createOrderLegCollection(
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
   * Close the given legs.
   */
  private void closeLegs(Set<TickerSymbol> tickerSymbols, InstrumentType instrumentType) {
    log.info("Closing {} positions {}", instrumentType, tickerSymbols);
    if (simulated) {
      closeSimulatedPositions(tickerSymbols.stream().filter(ts -> ts.getType() == instrumentType).toList());
      return;
    }
    List<Position> positions = fetchAccount()
        .flatMapMany(account -> Flux.fromIterable(account.getSecuritiesAccount().getPositions()))
        .filter(position -> {
          var tickerSymbol = TickerSymbol.fromString(position.getInstrument().getSymbol());
          return tickerSymbols.contains(tickerSymbol) && instrumentType == tickerSymbol.getType();
        })
        .toStream()
        .toList();
    BigDecimal price =
        positions.stream().map(position -> {
          BigDecimal quantity =
              position.getLongQuantity() != null ? position.getLongQuantity() : position.getShortQuantity();
          return position.getMarketValue().multiply(quantity);
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    List<OrderLegCollection> orderLegCollections = positions.stream()
        .map(position -> {
          BigDecimal quantity =
              position.getLongQuantity() != null && position.getLongQuantity().compareTo(BigDecimal.ZERO) != 0 ?
                  position.getLongQuantity() : position.getShortQuantity();
          OrderLegCollection.Instruction instruction = quantity.compareTo(BigDecimal.ZERO) < 0 ?
              OrderLegCollection.Instruction.BUY_TO_CLOSE : OrderLegCollection.Instruction.SELL_TO_CLOSE;
          return createOrderLegCollection(position.getInstrument().getSymbol(), quantity.negate(), instruction);
        })
        .toList();

    Order order = new Order();
    order.setOrderLegCollection(orderLegCollections);
    order.setComplexOrderStrategyType(ComplexOrderStrategyType.VERTICAL);
    order.setDuration(Duration.DAY);
    order.setOrderType(OrderType.LIMIT);
    order.setPrice(price);
    placeOrder(order).subscribe();
  }

  /**
   * Close the remaining positions.
   */
  void closeRemainingPositions(Set<TickerSymbol> symbols) {
    log.info("Closing remaining positions...");
    if (simulated) {
      closeSimulatedPositions(symbols);
      return;
    }
    List<Position> positions =
        fetchAccount()
            .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)))
            //.log()
            .flatMapMany(account -> Flux.fromStream(account.getSecuritiesAccount().getPositions().stream()))
            .filter(position -> {
              var tickerSymbol = TickerSymbol.fromString(position.getInstrument().getSymbol());
              return symbols.contains(tickerSymbol);
            })
            .toStream()
            .toList();
    List<OrderLegCollection> orderLegCollections = positions.stream()
        .map(position -> {
          BigDecimal quantity =
              position.getLongQuantity() == null && position.getLongQuantity().compareTo(BigDecimal.ZERO) != 0 ?
                  position.getLongQuantity() : position.getShortQuantity();
          OrderLegCollection.Instruction instruction = quantity.compareTo(BigDecimal.ZERO) < 0 ?
              OrderLegCollection.Instruction.BUY_TO_CLOSE : OrderLegCollection.Instruction.SELL_TO_CLOSE;
          return createOrderLegCollection(position.getInstrument().getSymbol(), quantity.negate(), instruction);
        })
        .toList();

    Order order = new Order();
    order.setOrderLegCollection(orderLegCollections);
    order.setOrderType(OrderType.MARKET);
    order.setDuration(Duration.DAY);
    placeOrder(order).subscribe();
  }

  /**
   * Convert a collection of {@link TickerSymbol}s into a collection of symbols.
   * @param tickerSymbols a collection of {@link TickerSymbol}s.
   * @return a collection of the symbols.
   */
  private Collection<String> tickerSymbolsToSymbols(Collection<TickerSymbol> tickerSymbols) {
    return tickerSymbols.stream().map(TickerSymbol::getSymbol).toList();
  }

  /**
   * Close simulated positions.
   * @param tickerSymbols close positions that match the given {@link TickerSymbol}s.
   */
  private void closeSimulatedPositions(Collection<TickerSymbol> tickerSymbols) {
    Collection<String> symbols = tickerSymbolsToSymbols(tickerSymbols);
    Map<String, BigDecimal> quotes =
        getQuotes(symbols).toStream().collect(Collectors.toMap(QuoteResponse::getSymbol, this::getMark));
    LocalDateTime now = LocalDateTime.now();
    positionRepository.findBySymbolIn(symbols).stream().peek(position -> {
      position.setClosed(now);
      position.setSellPrice(quotes.get(position.getSymbol()));
    }).forEach(positionRepository::save);
    openPositionIds.get().removeAll(tickerSymbols);
    closedPositionIds.get().addAll(tickerSymbols);
  }

  /**
   * Find option contracts with the given delta and option type.
   * @param options a list of option contracts.
   * @param delta the target delta.
   * @param putCall the type of contract, PUT or CALL.
   * @return the option contract that most closely matches the given parameters.
   */
  private Optional<OptionContract> findOption(
      List<OptionContract> options,
      BigDecimal delta,
      OptionContract.PutCall putCall) {

    return options.stream()
        .filter(option -> option.getPutCall() == putCall)
        .filter(not(option -> Objects.equals(option.getDelta(), BigDecimal.ZERO)))
        .min(Comparator.comparing(option -> option.getDelta().subtract(delta).abs()));
  }

  /**
   * Initialize the state of this service.
   */
  @PostConstruct
  private void init() {
    log.info("Initializing ZeroDTE agent...");
    if (!(accountsAndTradingClient.isInitialized() && marketDataClient.isInitialized())) {
      List<SessionEntity> sessions = sessionRepository.findAll();
      SchwabAccount schwabAccount = new SchwabAccount();
      schwabAccount.setUserId(userId);
      if (!sessions.isEmpty()) {
        SessionEntity session = sessions.getFirst();
        schwabAccount.setAccessToken(session.getToken());
        schwabAccount.setAccessExpiration(session.getAccessExpiration());
        schwabAccount.setRefreshToken(session.getRefreshToken());
        schwabAccount.setRefreshExpiration(session.getRefreshExpiration());
      }
      if (!accountsAndTradingClient.isInitialized()) {
        accountsAndTradingClient.init(schwabAccount, schwabApiClientTokenService);
      }
      if (!marketDataClient.isInitialized()) {
        marketDataClient.init(schwabAccount, schwabApiClientTokenService);
      }
    }
    Set<TickerSymbol> symbols = positionRepository.findByClosedIsNull().stream()
        .map(PositionEntity::getSymbol)
        .map(TickerSymbol::fromString).collect(Collectors.toSet());
    openPositionIds.get().addAll(symbols);

    List<PositionEntity> closedPositions =
        positionRepository.findByClosedNotNullAndCreatedIsAfter(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS));
    closedPositionIds.get().addAll(closedPositions.stream()
        .map(PositionEntity::getSymbol).map(TickerSymbol::fromString).collect(Collectors.toSet()));

    // Positions that were opened on a previous day would have expired
    List<PositionEntity> openPositions = positionRepository.findByClosedIsNull();
    openPositionIds.get().addAll(openPositions.stream()
        .map(PositionEntity::getSymbol).map(TickerSymbol::fromString).collect(Collectors.toSet()));

  }

  /**
   * Fetch the encrypted hash value of this account.
   * @return encrypted account hash value.
   */
  private String getEncryptedAccountHash() {
    if (encryptedAccountHash == null) {
      encryptedAccountHash = accountsAndTradingClient.fetchEncryptedAccountsToFlux(userId)
          .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2)))
          .toStream()
          .filter(account -> Objects.equals(account.getAccountNumber(), accountNumber))
          .findFirst().orElseThrow().getHashValue();
    }
    return encryptedAccountHash;
  }
}
