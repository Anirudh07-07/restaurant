package com.foodnest.foodnest.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private Long id;
    private String houseNumber;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String landmark;
    private String phoneNumber;
    private boolean isDefault;
}
