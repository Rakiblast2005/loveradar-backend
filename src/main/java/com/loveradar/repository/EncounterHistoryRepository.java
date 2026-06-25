package com.loveradar.repository;

import com.loveradar.entity.Contact;
import com.loveradar.entity.EncounterHistory;
import com.loveradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterHistoryRepository extends JpaRepository<EncounterHistory, java.util.UUID> {
    Optional<EncounterHistory> findByUserAndContact(User user, Contact contact);
    List<EncounterHistory> findByUserOrderByEncounterCountDesc(User user);
}
