package com.minishop.inventoryservice.repository;

import com.minishop.inventoryservice.entity.ReservationStatus;
import com.minishop.inventoryservice.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant timestamp);
}
