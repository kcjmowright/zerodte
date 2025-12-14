package com.kcjmowright.zerodte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ForwardController {

  @RequestMapping(value = "/{path:(?!api|static|public|assets)[^.]*}/**")
  public String forward() {
    return "forward:/";
  }
}