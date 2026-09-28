package com.kaveri.service;

import com.kaveri.dto.request.ContactRequest;

public interface ContactService {

    void handleContactSubmission(ContactRequest request);
}
