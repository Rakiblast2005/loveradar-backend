package com.loveradar.repository;

import com.loveradar.entity.CoupleSession;
import com.loveradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<CoupleSession, UUID> {
    Optional<CoupleSession> findBySessionCode(String sessionCode);

    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM CoupleSession s WHERE s.active = true AND (s.creator = :user OR s.partner = :user)")
    Optional<CoupleSession> findActiveSessionForUser(User user);

    List<CoupleSession> findByCreatorOrPartner(User creator, User partner);
}
