package com.kcjmowright.zerodte.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final StockPriceService stockPriceService;

  @EventListener
  public void handleSubscribeEvent(SessionSubscribeEvent event) {
    StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
    String destination = headers.getDestination();

    if (destination != null && destination.startsWith("/topic/stock/")) {
      String symbol = destination.replace("/topic/stock/", "");
      stockPriceService.addSymbol(symbol);
    }
  }

  @EventListener
  public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
    // Handle cleanup if needed
  }
}
