package com.kaveri.service;

import com.kaveri.dto.request.LoginRequest;
import com.kaveri.dto.request.RegisterRequest;
import com.kaveri.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
