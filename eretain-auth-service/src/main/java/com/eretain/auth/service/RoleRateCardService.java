package com.eretain.auth.service;

import com.eretain.auth.dto.RoleRateCardDTO;
import com.eretain.auth.entity.RoleRateCard;
import com.eretain.auth.repository.RoleRateCardRepository;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleRateCardService {

    private final RoleRateCardRepository repository;

    public List<RoleRateCardDTO> getAllRateCards() {
        return repository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<String> getActiveRoleNames() {
        return repository.findActiveRoleNames();
    }

    public RoleRateCardDTO getRateCardById(Long id) {
        return mapToDTO(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoleRateCard", "id", id)));
    }

    @Transactional
    public RoleRateCardDTO createRateCard(RoleRateCardDTO dto) {
        if (repository.existsByRoleName(dto.getRoleName())) {
            throw new BusinessValidationException("Rate card already exists for role: " + dto.getRoleName());
        }

        RoleRateCard entity = RoleRateCard.builder()
                .roleName(dto.getRoleName())
                .audaxRate(dto.getAudaxRate())
                .fixedFeeRate(dto.getFixedFeeRate())
                .tmRate(dto.getTmRate())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .description(dto.getDescription())
                .build();

        return mapToDTO(repository.save(entity));
    }

    @Transactional
    public RoleRateCardDTO updateRateCard(Long id, RoleRateCardDTO dto) {
        RoleRateCard entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoleRateCard", "id", id));

        if (dto.getRoleName() != null) entity.setRoleName(dto.getRoleName());
        if (dto.getAudaxRate() != null) entity.setAudaxRate(dto.getAudaxRate());
        if (dto.getFixedFeeRate() != null) entity.setFixedFeeRate(dto.getFixedFeeRate());
        if (dto.getTmRate() != null) entity.setTmRate(dto.getTmRate());
        if (dto.getCurrency() != null) entity.setCurrency(dto.getCurrency());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());

        return mapToDTO(repository.save(entity));
    }

    @Transactional
    public void deleteRateCard(Long id) {
        RoleRateCard entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoleRateCard", "id", id));
        entity.setActive(false);
        repository.save(entity);
    }

    private RoleRateCardDTO mapToDTO(RoleRateCard entity) {
        return RoleRateCardDTO.builder()
                .id(entity.getId())
                .roleName(entity.getRoleName())
                .audaxRate(entity.getAudaxRate())
                .fixedFeeRate(entity.getFixedFeeRate())
                .tmRate(entity.getTmRate())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .active(entity.isActive())
                .build();
    }
}
