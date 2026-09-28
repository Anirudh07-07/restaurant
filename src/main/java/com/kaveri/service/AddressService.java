package com.kaveri.service;

import com.kaveri.dto.request.AddressRequest;
import com.kaveri.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getMyAddresses();

    AddressResponse addAddress(AddressRequest request);

    AddressResponse updateAddress(Long id, AddressRequest request);

    void deleteAddress(Long id);

    AddressResponse setDefault(Long id);
}
