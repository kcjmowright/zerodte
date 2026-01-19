package com.kcjmowright.zerodte.controller;

import com.kcjmowright.zerodte.model.StockPriceUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StockWebSocketController {

  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/subscribe.stock")
  @SendTo("/topic/stock")
  public void subscribeToStock(@Payload String symbol) {
    // Client subscription handled by STOMP
  }

  // Send updates to all subscribers for a specific symbol
  public void sendStockUpdate(String symbol, StockPriceUpdate update) {
    messagingTemplate.convertAndSend("/topic/stock/" + symbol, update);
  }
}
