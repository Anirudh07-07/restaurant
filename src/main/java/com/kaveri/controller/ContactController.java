package com.kaveri.controller;

import com.kaveri.dto.request.ContactRequest;
import com.kaveri.dto.response.ApiResponse;
import com.kaveri.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "Public customer inquiries and feedback")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Submit a contact inquiry (public)")
    public ResponseEntity<ApiResponse<Void>> submitContactForm(
            @Valid @RequestBody ContactRequest request) {
        contactService.handleContactSubmission(request);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Thank you, " + request.getSenderName() + "! Your message has been received. Our team will contact you shortly."));
    }
}
