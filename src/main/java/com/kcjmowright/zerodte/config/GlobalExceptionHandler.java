package com.kcjmowright.zerodte.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(HttpMessageNotWritableException.class)
  public ResponseEntity<?> handleNotWritable(HttpMessageNotWritableException ex) {
    log.error(Arrays.stream(ex.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
    return ResponseEntity.status(500).body("Serialization error");
  }
}
