package com.eretain.company.service;

import com.eretain.company.dto.*;
import com.eretain.company.entity.*;
import com.eretain.company.repository.*;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final BusinessUnitRepository businessUnitRepository;
    private final UnitRepository unitRepository;
    private final DeliveryUnitRepository deliveryUnitRepository;

    // =================== Business Unit ===================

    public List<BusinessUnitDTO> getAllBusinessUnits() {
        return businessUnitRepository.findByActiveTrue().stream()
                .map(this::mapBusinessUnitToDTO)
                .collect(Collectors.toList());
    }

    public BusinessUnitDTO getBusinessUnitById(Long id) {
        return mapBusinessUnitToDTO(businessUnitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id)));
    }

    @Transactional
    public BusinessUnitDTO createBusinessUnit(BusinessUnitDTO dto) {
        if (businessUnitRepository.existsByCode(dto.getCode())) {
            throw new BusinessValidationException("Business unit code already exists: " + dto.getCode());
        }
        BusinessUnit entity = BusinessUnit.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return mapBusinessUnitToDTO(businessUnitRepository.save(entity));
    }

    @Transactional
    public BusinessUnitDTO updateBusinessUnit(Long id, BusinessUnitDTO dto) {
        BusinessUnit entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        return mapBusinessUnitToDTO(businessUnitRepository.save(entity));
    }

    @Transactional
    public void deleteBusinessUnit(Long id) {
        BusinessUnit entity = businessUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
        entity.setActive(false);
        businessUnitRepository.save(entity);
    }

    // =================== Unit ===================

    public List<UnitDTO> getAllUnits() {
        return unitRepository.findByActiveTrue().stream()
                .map(this::mapUnitToDTO)
                .collect(Collectors.toList());
    }

    public List<UnitDTO> getUnitsByBusinessUnit(Long businessUnitId) {
        return unitRepository.findByBusinessUnitIdAndActiveTrue(businessUnitId).stream()
                .map(this::mapUnitToDTO)
                .collect(Collectors.toList());
    }

    public UnitDTO getUnitById(Long id) {
        return mapUnitToDTO(unitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", id)));
    }

    @Transactional
    public UnitDTO createUnit(UnitDTO dto) {
        BusinessUnit bu = businessUnitRepository.findById(dto.getBusinessUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", dto.getBusinessUnitId()));

        if (unitRepository.existsByCodeAndBusinessUnitId(dto.getCode(), dto.getBusinessUnitId())) {
            throw new BusinessValidationException("Unit code already exists in this business unit");
        }

        Unit entity = Unit.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .businessUnit(bu)
                .build();
        return mapUnitToDTO(unitRepository.save(entity));
    }

    @Transactional
    public UnitDTO updateUnit(Long id, UnitDTO dto) {
        Unit entity = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", id));
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        return mapUnitToDTO(unitRepository.save(entity));
    }

    @Transactional
    public void deleteUnit(Long id) {
        Unit entity = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", id));
        entity.setActive(false);
        unitRepository.save(entity);
    }

    // =================== Delivery Unit ===================

    public List<DeliveryUnitDTO> getAllDeliveryUnits() {
        return deliveryUnitRepository.findByActiveTrue().stream()
                .map(this::mapDeliveryUnitToDTO)
                .collect(Collectors.toList());
    }

    public List<DeliveryUnitDTO> getDeliveryUnitsByUnit(Long unitId) {
        return deliveryUnitRepository.findByUnitIdAndActiveTrue(unitId).stream()
                .map(this::mapDeliveryUnitToDTO)
                .collect(Collectors.toList());
    }

    public DeliveryUnitDTO getDeliveryUnitById(Long id) {
        return mapDeliveryUnitToDTO(deliveryUnitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryUnit", "id", id)));
    }

    @Transactional
    public DeliveryUnitDTO createDeliveryUnit(DeliveryUnitDTO dto) {
        Unit unit = unitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit", "id", dto.getUnitId()));

        if (deliveryUnitRepository.existsByCodeAndUnitId(dto.getCode(), dto.getUnitId())) {
            throw new BusinessValidationException("Delivery unit code already exists in this unit");
        }

        DeliveryUnit entity = DeliveryUnit.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .unit(unit)
                .build();
        return mapDeliveryUnitToDTO(deliveryUnitRepository.save(entity));
    }

    @Transactional
    public DeliveryUnitDTO updateDeliveryUnit(Long id, DeliveryUnitDTO dto) {
        DeliveryUnit entity = deliveryUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryUnit", "id", id));
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        return mapDeliveryUnitToDTO(deliveryUnitRepository.save(entity));
    }

    @Transactional
    public void deleteDeliveryUnit(Long id) {
        DeliveryUnit entity = deliveryUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryUnit", "id", id));
        entity.setActive(false);
        deliveryUnitRepository.save(entity);
    }

    // =================== Mappers ===================

    private BusinessUnitDTO mapBusinessUnitToDTO(BusinessUnit entity) {
        List<UnitDTO> units = entity.getUnits() != null
                ? entity.getUnits().stream().filter(u -> u.isActive()).map(this::mapUnitToDTO).collect(Collectors.toList())
                : null;
        return BusinessUnitDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .units(units)
                .build();
    }

    private UnitDTO mapUnitToDTO(Unit entity) {
        List<DeliveryUnitDTO> deliveryUnits = entity.getDeliveryUnits() != null
                ? entity.getDeliveryUnits().stream().filter(du -> du.isActive()).map(this::mapDeliveryUnitToDTO).collect(Collectors.toList())
                : null;
        return UnitDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .businessUnitId(entity.getBusinessUnit().getId())
                .businessUnitName(entity.getBusinessUnit().getName())
                .active(entity.isActive())
                .deliveryUnits(deliveryUnits)
                .build();
    }

    private DeliveryUnitDTO mapDeliveryUnitToDTO(DeliveryUnit entity) {
        return DeliveryUnitDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .unitId(entity.getUnit().getId())
                .unitName(entity.getUnit().getName())
                .active(entity.isActive())
                .build();
    }
}
