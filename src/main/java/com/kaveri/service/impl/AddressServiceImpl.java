package com.kaveri.service.impl;

import com.kaveri.dto.request.AddressRequest;
import com.kaveri.dto.response.AddressResponse;
import com.kaveri.entity.Address;
import com.kaveri.entity.User;
import com.kaveri.exception.ResourceNotFoundException;
import com.kaveri.exception.UnauthorizedException;
import com.kaveri.repository.AddressRepository;
import com.kaveri.service.AddressService;
import com.kaveri.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        User user = securityUtils.getCurrentUser();
        return addressRepository.findByUserId(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = securityUtils.getCurrentUser();

        // If this is default, unset previous default
        if (request.isDefault()) {
            unsetCurrentDefault(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .houseNumber(request.getHouseNumber())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .landmark(request.getLandmark())
                .phoneNumber(request.getPhoneNumber())
                .isDefault(request.isDefault())
                .build();

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long id, AddressRequest request) {
        User user = securityUtils.getCurrentUser();
        Address address = findOwnedAddress(id, user.getId());

        if (request.isDefault()) {
            unsetCurrentDefault(user.getId());
        }

        address.setHouseNumber(request.getHouseNumber());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setDefault(request.isDefault());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        User user = securityUtils.getCurrentUser();
        findOwnedAddress(id, user.getId()); // throws if not owned
        addressRepository.deleteByIdAndUserId(id, user.getId());
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long id) {
        User user = securityUtils.getCurrentUser();
        unsetCurrentDefault(user.getId());
        Address address = findOwnedAddress(id, user.getId());
        address.setDefault(true);
        return toResponse(addressRepository.save(address));
    }

    private void unsetCurrentDefault(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
            addr.setDefault(false);
            addressRepository.save(addr);
        });
    }

    private Address findOwnedAddress(Long id, Long userId) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));
        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("This address does not belong to you");
        }
        return address;
    }

    private AddressResponse toResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .houseNumber(a.getHouseNumber())
                .street(a.getStreet())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .landmark(a.getLandmark())
                .phoneNumber(a.getPhoneNumber())
                .isDefault(a.isDefault())
                .build();
    }
}
