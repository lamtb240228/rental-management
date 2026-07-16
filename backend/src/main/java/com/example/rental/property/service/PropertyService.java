package com.example.rental.property.service;

import com.example.rental.common.exception.NotFoundException;
import com.example.rental.common.security.CurrentUserService;
import com.example.rental.common.exception.BadRequestException;
import com.example.rental.contract.entity.ContractStatus;
import com.example.rental.contract.repository.RentalContractRepository;
import com.example.rental.property.entity.RoomStatus;
import com.example.rental.property.repository.RoomRepository;
import com.example.rental.property.dto.PropertyRequest;
import com.example.rental.property.dto.PropertyResponse;
import com.example.rental.property.entity.Property;
import com.example.rental.property.entity.PropertyStatus;
import com.example.rental.property.mapper.PropertyMapper;
import com.example.rental.property.repository.PropertyRepository;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.repository.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final RoomRepository roomRepository;
    private final RentalContractRepository contractRepository;

    public PropertyService(
        PropertyRepository propertyRepository,
        UserAccountRepository userAccountRepository,
        CurrentUserService currentUserService,
        RoomRepository roomRepository,
        RentalContractRepository contractRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUserService = currentUserService;
        this.roomRepository = roomRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> listMine() {
        Long landlordId = currentUserService.currentUserId();
        return propertyRepository.findByLandlordIdAndDeletedAtIsNullOrderByCreatedAtDesc(landlordId)
            .stream()
            .map(PropertyMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long id) {
        return PropertyMapper.toResponse(getOwnedProperty(id));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request) {
        Long landlordId = currentUserService.currentUserId();
        UserAccount landlord = userAccountRepository.findById(landlordId)
            .orElseThrow(() -> new NotFoundException("Current user not found"));

        Property property = new Property();
        property.setLandlord(landlord);
        PropertyMapper.apply(property, request);
        propertyRepository.save(property);
        return PropertyMapper.toResponse(property);
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyRequest request) {
        Property property = getOwnedPropertyForUpdate(id);
        if (request.status() == PropertyStatus.INACTIVE
            && contractRepository.existsByRoomPropertyIdAndStatusAndDeletedAtIsNull(id, ContractStatus.ACTIVE)) {
            throw new BadRequestException("Cannot deactivate a property with active contracts");
        }
        PropertyMapper.apply(property, request);
        return PropertyMapper.toResponse(property);
    }

    @Transactional
    public void delete(Long id) {
        Property property = getOwnedPropertyForUpdate(id);
        if (contractRepository.existsByRoomPropertyIdAndStatusAndDeletedAtIsNull(id, ContractStatus.ACTIVE)) {
            throw new BadRequestException("Cannot remove a property with active contracts");
        }
        roomRepository.findByPropertyIdAndDeletedAtIsNullOrderByRoomNumber(id).forEach(room -> {
            room.setStatus(RoomStatus.INACTIVE);
            room.softDelete();
        });
        property.softDelete();
    }

    public Property getOwnedProperty(Long id) {
        Long landlordId = currentUserService.currentUserId();
        return propertyRepository.findByIdAndLandlordIdAndDeletedAtIsNull(id, landlordId)
            .orElseThrow(() -> new NotFoundException("Property not found"));
    }

    public Property getOwnedPropertyForUpdate(Long id) {
        Long landlordId = currentUserService.currentUserId();
        return propertyRepository.findOwnedByIdForUpdate(id, landlordId)
            .orElseThrow(() -> new NotFoundException("Property not found"));
    }
}
