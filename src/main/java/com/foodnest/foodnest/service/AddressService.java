package com.foodnest.foodnest.service;

import com.foodnest.foodnest.dto.request.AddressRequest;
import com.foodnest.foodnest.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getMyAddresses();

    AddressResponse addAddress(AddressRequest request);

    AddressResponse updateAddress(Long id, AddressRequest request);

    void deleteAddress(Long id);

    AddressResponse setDefault(Long id);
}
