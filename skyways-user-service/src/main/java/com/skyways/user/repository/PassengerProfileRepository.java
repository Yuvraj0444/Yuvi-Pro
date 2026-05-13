package com.skyways.user.repository;

import com.skyways.user.entity.PassengerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PassengerProfileRepository extends JpaRepository<PassengerProfile, UUID> {
    List<PassengerProfile> findByUserUserId(UUID userId);
}
