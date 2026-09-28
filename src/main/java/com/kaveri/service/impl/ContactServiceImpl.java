package com.kaveri.service.impl;

import com.kaveri.dto.request.ContactRequest;
import com.kaveri.service.ContactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ContactServiceImpl implements ContactService {

    @Override
    public void handleContactSubmission(ContactRequest request) {
        log.info("New contact inquiry received from '{}' <{}> regarding '{}': {}",
                request.getSenderName(),
                request.getSenderEmail(),
                request.getVentureDepartment(),
                request.getSenderMessage());
    }
}
