package com.kcjmowright.zerodte.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class ForwardController {

  @RequestMapping(value = "/{path:(?!api|static|public|assets)[^.]*}/**")
  public String forward(@PathVariable String path) {
    log.debug("Forwarding path: {} to /", path);
    return "forward:/";
  }
}