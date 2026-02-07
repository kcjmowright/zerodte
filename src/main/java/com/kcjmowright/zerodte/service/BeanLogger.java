package com.kcjmowright.zerodte.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.stream.Collectors;

//@Component
@Slf4j
public class BeanLogger {

  private final ApplicationContext context;

  public BeanLogger(ApplicationContext context) {
    this.context = context;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logBeans() {
    log.info("Registered Beans\n{}",
        Arrays.stream(context.getBeanDefinitionNames()).sorted()
            .map(name -> String.format("%s : %s", name, context.getBean(name).getClass().getCanonicalName()))
            .collect(Collectors.joining("\n")));
  }
}