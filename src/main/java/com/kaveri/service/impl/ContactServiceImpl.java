package com.kaveri.service.impl;

import com.kaveri.dto.request.ContactRequest;
import com.kaveri.entity.ContactMessage;
import com.kaveri.repository.ContactMessageRepository;
import com.kaveri.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactMessageRepository contactMessageRepository;

    @Override
    @Transactional
    public void handleContactSubmission(ContactRequest request) {
        log.info("New contact inquiry received from '{}' <{}> regarding '{}'",
                request.getSenderName(),
                request.getSenderEmail(),
                request.getVentureDepartment());

        ContactMessage msg = ContactMessage.builder()
                .senderName(request.getSenderName() != null ? request.getSenderName().trim() : "Guest")
                .senderEmail(request.getSenderEmail() != null ? request.getSenderEmail().trim() : "")
                .senderPhone(request.getSenderPhone() != null ? request.getSenderPhone().trim() : "")
                .ventureDepartment(request.getVentureDepartment() != null ? request.getVentureDepartment().trim() : "General Inquiry")
                .senderMessage(request.getSenderMessage() != null ? request.getSenderMessage().trim() : "")
                .isRead(false)
                .build();

        contactMessageRepository.save(msg);
    }
}
