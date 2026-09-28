package com.foodnest.foodnest.service;

import com.foodnest.foodnest.dto.request.LoginRequest;
import com.foodnest.foodnest.dto.request.RegisterRequest;
import com.foodnest.foodnest.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
