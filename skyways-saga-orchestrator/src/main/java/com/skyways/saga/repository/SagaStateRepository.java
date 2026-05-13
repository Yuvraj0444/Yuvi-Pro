package com.skyways.saga.repository;

import com.skyways.saga.entity.SagaState;
import com.skyways.saga.entity.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    Optional<SagaState> findByBookingId(UUID bookingId);
    boolean existsBySagaIdAndStatus(UUID sagaId, SagaStatus status);
}
