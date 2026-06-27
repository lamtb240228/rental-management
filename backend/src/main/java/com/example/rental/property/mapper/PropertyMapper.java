package com.example.rental.property.mapper;

import com.example.rental.property.dto.PropertyRequest;
import com.example.rental.property.dto.PropertyResponse;
import com.example.rental.property.entity.Property;
import com.example.rental.property.entity.PropertyStatus;

public final class PropertyMapper {
    private PropertyMapper() {
    }

    public static void apply(Property property, PropertyRequest request) {
        property.setName(request.name().trim());
        property.setAddressLine(request.addressLine().trim());
        property.setWard(request.ward());
        property.setDistrict(request.district());
        property.setProvinceCity(request.provinceCity().trim());
        property.setDescription(request.description());
        property.setStatus(request.status() == null ? PropertyStatus.ACTIVE : request.status());
    }

    public static PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
            property.getId(),
            property.getName(),
            property.getAddressLine(),
            property.getWard(),
            property.getDistrict(),
            property.getProvinceCity(),
            property.getDescription(),
            property.getStatus(),
            property.getCreatedAt(),
            property.getUpdatedAt()
        );
    }
}
