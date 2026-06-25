package com.loveradar.repository;

import com.loveradar.entity.Contact;
import com.loveradar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByOwner(User owner);
    Optional<Contact> findByIdAndOwner(UUID id, User owner);
    List<Contact> findByOwnerAndTrustedTrue(User owner);
    List<Contact> findByLinkedUser(User linkedUser);
}
