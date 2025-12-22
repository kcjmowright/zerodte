package com.kcjmowright.zerodte.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
    if (request.getRequestURI().contains("/oauth2/schwab/authorization")) {
      var userid = request.getParameter("schwabUserId");
      // Session cookie
      Cookie cookie = new Cookie("zun", userid);
      cookie.setHttpOnly(true);
      cookie.setSecure(true);
      cookie.setPath("/");
      cookie.setMaxAge(-1);
      response.addCookie(cookie);
    }
    chain.doFilter(request, response);
  }
}
