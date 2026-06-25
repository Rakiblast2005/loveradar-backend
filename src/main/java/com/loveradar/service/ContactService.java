package com.loveradar.service;

import com.loveradar.dto.contact.ContactRequest;
import com.loveradar.dto.contact.ContactResponse;
import com.loveradar.entity.Contact;
import com.loveradar.entity.EncounterHistory;
import com.loveradar.entity.User;
import com.loveradar.exception.ResourceNotFoundException;
import com.loveradar.repository.ContactRepository;
import com.loveradar.repository.EncounterHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final EncounterHistoryRepository encounterHistoryRepository;
    private final UserService userService;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts() {
        User owner = userService.getCurrentUser();
        return contactRepository.findByOwner(owner).stream()
                .map(contact -> toResponse(contact, owner))
                .toList();
    }

    @Transactional
    public ContactResponse addContact(ContactRequest request) {
        User owner = userService.getCurrentUser();

        Contact contact = Contact.builder()
                .owner(owner)
                .contactName(request.getContactName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .trusted(request.isTrusted())
                .build();

        // Best-effort: link to a registered LoveRadar user if their account
        // email happens to match, or could be extended to phone matching
        // once phone numbers are stored on User accounts.
        contact = contactRepository.save(contact);

        return toResponse(contact, owner);
    }

    @Transactional
    public ContactResponse updateContact(UUID contactId, ContactRequest request) {
        User owner = userService.getCurrentUser();
        Contact contact = contactRepository.findByIdAndOwner(contactId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        contact.setContactName(request.getContactName().trim());
        contact.setPhoneNumber(request.getPhoneNumber().trim());
        contact.setTrusted(request.isTrusted());

        contact = contactRepository.save(contact);
        return toResponse(contact, owner);
    }

    @Transactional
    public void deleteContact(UUID contactId) {
        User owner = userService.getCurrentUser();
        Contact contact = contactRepository.findByIdAndOwner(contactId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        contactRepository.delete(contact);
    }

    private ContactResponse toResponse(Contact contact, User owner) {
        EncounterHistory history = encounterHistoryRepository
                .findByUserAndContact(owner, contact)
                .orElse(null);

        return ContactResponse.builder()
                .id(contact.getId().toString())
                .contactName(contact.getContactName())
                .phoneNumber(contact.getPhoneNumber())
                .trusted(contact.isTrusted())
                .linkedToUser(contact.getLinkedUser() != null)
                .encounterCount(history != null ? history.getEncounterCount() : 0)
                .lastEncountered(history != null && history.getLastEncountered() != null
                        ? history.getLastEncountered().format(ISO) : null)
                .build();
    }
}
