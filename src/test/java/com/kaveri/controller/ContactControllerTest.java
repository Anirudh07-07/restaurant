package com.kaveri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaveri.dto.request.ContactRequest;
import com.kaveri.service.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ContactController Integration Tests")
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContactService contactService;

    @Test
    @DisplayName("POST /api/contact - Valid request returns 200")
    void submitContactForm_validRequest_returns200() throws Exception {
        ContactRequest request = ContactRequest.builder()
                .senderName("Rohan Verma")
                .senderEmail("rohan@example.com")
                .senderPhone("+91-8235520520")
                .ventureDepartment("Outdoor Catering / Banquet Inquiry")
                .senderMessage("Inquiry regarding banquet booking for 150 guests.")
                .build();

        doNothing().when(contactService).handleContactSubmission(any(ContactRequest.class));

        mockMvc.perform(post("/api/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Thank you, Rohan Verma!")));
    }

    @Test
    @DisplayName("POST /api/contact - Missing required fields returns 400")
    void submitContactForm_missingFields_returns400() throws Exception {
        ContactRequest request = ContactRequest.builder()
                .senderName("")
                .senderEmail("not-an-email")
                .build();

        mockMvc.perform(post("/api/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
