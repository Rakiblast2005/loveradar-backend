package com.loveradar.service;

import com.loveradar.dto.contact.ContactRequest;
import com.loveradar.dto.contact.ContactResponse;
import com.loveradar.entity.Contact;
import com.loveradar.entity.Role;
import com.loveradar.entity.User;
import com.loveradar.exception.ResourceNotFoundException;
import com.loveradar.repository.ContactRepository;
import com.loveradar.repository.EncounterHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactService")
class ContactServiceTest {

    @Mock ContactRepository contactRepository;
    @Mock EncounterHistoryRepository encounterHistoryRepository;
    @Mock UserService userService;

    @InjectMocks ContactService contactService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(UUID.randomUUID()).name("Alice").email("a@a.com")
                .password("x").role(Role.USER).build();
        when(userService.getCurrentUser()).thenReturn(owner);
    }

    @Test
    @DisplayName("getContacts() returns all contacts for owner")
    void getContacts() {
        Contact c = Contact.builder().id(UUID.randomUUID()).owner(owner)
                .contactName("Bob").phoneNumber("+1234567890").trusted(true).build();
        when(contactRepository.findByOwner(owner)).thenReturn(List.of(c));
        when(encounterHistoryRepository.findByUserAndContact(any(), any())).thenReturn(Optional.empty());

        List<ContactResponse> result = contactService.getContacts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContactName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("addContact() saves and returns new contact")
    void addContact() {
        ContactRequest req = new ContactRequest();
        req.setContactName("Bob");
        req.setPhoneNumber("+1234567890");
        req.setTrusted(true);

        Contact saved = Contact.builder().id(UUID.randomUUID()).owner(owner)
                .contactName("Bob").phoneNumber("+1234567890").trusted(true).build();
        when(contactRepository.save(any())).thenReturn(saved);
        when(encounterHistoryRepository.findByUserAndContact(any(), any())).thenReturn(Optional.empty());

        ContactResponse response = contactService.addContact(req);

        assertThat(response.getContactName()).isEqualTo("Bob");
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    @DisplayName("deleteContact() throws ResourceNotFoundException for wrong owner")
    void deleteWrongOwner() {
        UUID id = UUID.randomUUID();
        when(contactRepository.findByIdAndOwner(id, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.deleteContact(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
