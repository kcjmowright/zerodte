package com.kcjmowright.zerodte.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kcjmowright.zerodte.model.entity.OrderEntity;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

  List<OrderEntity> findAllByFilledIsNull();
}
