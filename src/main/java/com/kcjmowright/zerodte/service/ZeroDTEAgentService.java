package com.kcjmowright.zerodte.service;

import com.kcjmowright.zerodte.model.TickerSymbol;
import com.kcjmowright.zerodte.model.entity.OrderEntity;
import com.kcjmowright.zerodte.model.entity.PositionEntity;
import com.kcjmowright.zerodte.model.entity.QuoteEntity;
import com.kcjmowright.zerodte.repository.OrderRepository;
import com.kcjmowright.zerodte.repository.PositionRepository;
import com.kcjmowright.zerodte.repository.QuoteRepository;
import com.pangility.schwab.api.client.accountsandtrading.model.account.Position;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Duration;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Order;
import com.pangility.schwab.api.client.accountsandtrading.model.order.OrderLegCollection;
import com.pangility.schwab.api.client.accountsandtrading.model.order.OrderType;
import com.pangility.schwab.api.client.accountsandtrading.model.order.Status;
import com.pangility.schwab.api.client.marketdata.model.quotes.QuoteResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZeroDTEAgentService {

  private static final LocalTime TRADE_TIME_OPEN = LocalTime.of(8, 45); // 8:45 AM CST
  private static final LocalTime OPENING_TRADE_TIME_END = LocalTime.of(9, 30); // 9:30 AM CST
  private static final LocalTime CLOSE_TIME = LocalTime.of(15, 0); // 3:00 PM CST
  private static final long CLOSE_TIME_MINUTES_BEFORE = 10L; // Close all remaining positions 10 minutes before close
  private static final ZoneId CST = ZoneId.of("America/Chicago");

  private final AccountService accountService;
  private final OrderService orderService;
  private final PriceService priceService;
  private final OrderRepository orderRepository;
  private final PositionRepository positionRepository;
  private final QuoteRepository quoteRepository;

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

  /**
   * Execute the Zero Date Iron Condor Strategy.
   */
  @Scheduled(cron = "*/10 * 8-15 * * MON-FRI") // Every 10 seconds, every minute, every hour between 8 and 15 on weekdays.
  public void executeStrategy() {
    log.info("Executing strategy.  Open order id: {}, Open Positions: {}, Closed Positions: {}",
        openOrderId.get(), openPositionIds.get(), closedPositionIds.get());
    ZonedDateTime now = ZonedDateTime.now(CST);
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
   * Submit an IronCondor order.
   * @param symbol the underlying symbol.
   * @param fromDate the from date.
   * @param toDate the to date.
   */
  void submitIronCondorOrder(String symbol, LocalDate fromDate, LocalDate toDate) {
    Mono<Order> newOrder = orderService.buildIronCondorOrder(
        priceService.findIronCondor(
            symbol,
            fromDate,
            toDate,
            putLongDelta,
            putShortDelta,
            callShortDelta,
            callLongDelta),
        quantity
    );
    Mono<Order> placedOrder = simulated ?
        newOrder.map(order -> {
          order.setOrderId(new Random().nextLong());
          order.setEnteredTime(ZonedDateTime.now());
          return order;
        }) :
        newOrder
            .flatMap(order -> orderService.placeOrder(order)
                .thenMany(orderService.fetchOrders(null, null)).next());

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
      orderService.fetchOrder(orderIds.getFirst())
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
    log.info("Monitoring and possibly closing positions.");
    if (LocalTime.now().isAfter(CLOSE_TIME.minusMinutes(CLOSE_TIME_MINUTES_BEFORE))) {
      closeRemainingPositions(openPositionIds.get());
    }
    BigDecimal profitLoss =
        calculateCurrentProfitPercentage(openPositionIds.get().stream().map(TickerSymbol::getSymbol).toList());
    if (profitLoss.compareTo(profitTargetPercent) >= 0
          || profitLoss.compareTo(lossLimitPercent.negate()) <= 0) {
      closeRemainingPositions(openPositionIds.get());
    }
  }

  /**
   * Fetch positions from live account and persist them.
   * @param symbols filter by set of equity symbols.
   */
  void processOpenPositions(Set<String> symbols) {
    accountService.fetchAccount()
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
    Map<String, PositionEntity> positions = positionRepository.findByClosedIsNullAndSymbolIn(symbols).stream()
        .collect(toMap(PositionEntity::getSymbol, Function.identity()));
    BigDecimal purchasePrice = positions.values().stream()
        .map(position -> position.getPurchasePrice().multiply(position.getQuantity()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal currentPrice = priceService.getQuoteResponses(symbols).toStream()
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
      case QuoteResponse.IndexResponse ir -> ir.getQuote().getLastPrice();
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
    BigDecimal mark = getMark(priceService.getQuoteResponse(tickerSymbol.getSymbol()).block());
    positionEntity.setPurchasePrice(mark);
    positionEntity.setQuantity(order.getQuantity());
    openPositionIds.get().add(tickerSymbol);
    return positionEntity;
  }

//  /**
//   * Close the given legs.
//   */
//  private void closeLegs(Set<TickerSymbol> tickerSymbols, InstrumentType instrumentType) {
//    log.info("Closing {} positions {}", instrumentType, tickerSymbols);
//    if (simulated) {
//      closeSimulatedPositions(tickerSymbols.stream().filter(ts -> ts.getType() == instrumentType).toList());
//      return;
//    }
//    List<Position> positions = fetchAccount()
//        .flatMapMany(account -> Flux.fromIterable(account.getSecuritiesAccount().getPositions()))
//        .filter(position -> {
//          var tickerSymbol = TickerSymbol.fromString(position.getInstrument().getSymbol());
//          return tickerSymbols.contains(tickerSymbol) && instrumentType == tickerSymbol.getType();
//        })
//        .toStream()
//        .toList();
//    BigDecimal price =
//        positions.stream().map(position -> {
//          BigDecimal quantity =
//              position.getLongQuantity() != null ? position.getLongQuantity() : position.getShortQuantity();
//          return position.getMarketValue().multiply(quantity);
//        }).reduce(BigDecimal.ZERO, BigDecimal::add);
//    List<OrderLegCollection> orderLegCollections = positions.stream()
//        .map(position -> {
//          BigDecimal quantity =
//              position.getLongQuantity() != null && position.getLongQuantity().compareTo(BigDecimal.ZERO) != 0 ?
//                  position.getLongQuantity() : position.getShortQuantity();
//          OrderLegCollection.Instruction instruction = quantity.compareTo(BigDecimal.ZERO) < 0 ?
//              OrderLegCollection.Instruction.BUY_TO_CLOSE : OrderLegCollection.Instruction.SELL_TO_CLOSE;
//          return createOrderLegCollection(position.getInstrument().getSymbol(), quantity.negate(), instruction);
//        })
//        .toList();
//
//    Order order = new Order();
//    order.setOrderLegCollection(orderLegCollections);
//    order.setComplexOrderStrategyType(ComplexOrderStrategyType.VERTICAL);
//    order.setDuration(Duration.DAY);
//    order.setOrderType(OrderType.LIMIT);
//    order.setPrice(price);
//    placeOrder(order).subscribe();
//  }

  /**
   * Close the remaining positions.
   */
  void closeRemainingPositions(Set<TickerSymbol> symbols) {
    log.info("Closing positions");
    if (simulated) {
      closeSimulatedPositions(symbols);
      return;
    }
    List<Position> positions =
        accountService.fetchAccount()
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
          return orderService.createOrderLegCollection(position.getInstrument().getSymbol(), quantity.negate(), instruction);
        })
        .toList();

    Order order = new Order();
    order.setOrderLegCollection(orderLegCollections);
    order.setOrderType(OrderType.MARKET);
    order.setDuration(Duration.DAY);
    orderService.placeOrder(order).subscribe();
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
    Map<String, BigDecimal> quotes = priceService.getQuoteResponses(symbols)
        .toStream()
        .collect(toMap(QuoteResponse::getSymbol, this::getMark));
    LocalDateTime now = LocalDateTime.now();
    positionRepository.findByClosedIsNullAndSymbolIn(symbols).stream().peek(position -> {
      position.setClosed(now);
      position.setSellPrice(quotes.get(position.getSymbol()));
    }).forEach(positionRepository::save);
    openPositionIds.get().removeAll(tickerSymbols);
    closedPositionIds.get().addAll(tickerSymbols);
  }


  /**
   * Initialize the state of this service.
   */
  @PostConstruct
  private void init() {
    log.info("Initializing ZeroDTE agent...");
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
}
