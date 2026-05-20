package com.skyways.flight.repository;

import com.skyways.flight.entity.FareClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FareClassRepository extends JpaRepository<FareClass, UUID> {

    @Query("SELECT fc FROM FareClass fc WHERE fc.flight.flightId = :flightId AND fc.classType = :classType AND fc.available > 0")
    Optional<FareClass> findAvailableFare(@Param("flightId") UUID flightId, @Param("classType") String classType);

    @Query("SELECT fc FROM FareClass fc WHERE fc.flight.flightId IN :flightIds AND fc.classType = :classType AND fc.available > 0")
    List<FareClass> findAvailableFaresForFlights(@Param("flightIds") List<UUID> flightIds, @Param("classType") String classType);
}