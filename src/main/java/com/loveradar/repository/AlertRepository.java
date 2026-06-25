package com.loveradar.repository;

import com.loveradar.entity.Alert;
import com.loveradar.entity.AlertType;
import com.loveradar.entity.Contact;
import com.loveradar.entity.CoupleSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, java.util.UUID> {

    List<Alert> findBySessionOrderByCreatedAtDesc(CoupleSession session);

    List<Alert> findBySessionAndCreatedAtAfterOrderByCreatedAtDesc(CoupleSession session, LocalDateTime after);

    Optional<Alert> findFirstBySessionAndContactOrderByCreatedAtDesc(CoupleSession session, Contact contact);

    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM Alert a WHERE a.session.creator = :user OR a.session.partner = :user ORDER BY a.createdAt DESC")
    List<Alert> findAllForUser(com.loveradar.entity.User user);

    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM Alert a WHERE (a.session.creator = :user OR a.session.partner = :user) " +
        "AND a.alertType = :type AND a.createdAt >= :since")
    List<Alert> findForUserSince(com.loveradar.entity.User user, AlertType type, LocalDateTime since);
}
