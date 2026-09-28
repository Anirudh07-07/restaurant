package com.foodnest.foodnest.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String email;
    private String name;
    private String role;
    private Long userId;
}
