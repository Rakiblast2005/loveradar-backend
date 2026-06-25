package com.loveradar.repository;

import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.Location;
import com.loveradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findFirstByUserAndSessionOrderByTimestampDesc(User user, CoupleSession session);

    List<Location> findBySessionAndTimestampAfter(CoupleSession session, LocalDateTime after);

    List<Location> findByUserOrderByTimestampDesc(User user);

    @org.springframework.data.jpa.repository.Query(
        "SELECT l FROM Location l WHERE l.session = :session ORDER BY l.timestamp DESC")
    List<Location> findHistoryForSession(CoupleSession session);
}
