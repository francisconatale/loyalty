package com.loyalty.loyalty.service;

import com.loyalty.loyalty.domain.LoyaltyProgram;
import com.loyalty.loyalty.domain.LoyaltyProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyProgramService {

    private final LoyaltyProgramRepository programRepository;

    @Transactional
    public LoyaltyProgram createOrGetDefaultProgram(UUID tenantId, String name, String configJson) {
        return programRepository.findFirstByTenantIdAndEnabledTrue(tenantId)
                .orElseGet(() -> {
                    LoyaltyProgram program = new LoyaltyProgram(
                            UUID.randomUUID(),
                            tenantId,
                            name != null ? name : "Default Loyalty Program",
                            "POINTS",
                            configJson != null ? configJson : "{\"pointsPerSpent\": 100}"
                    );
                    return programRepository.save(program);
                });
    }

    @Transactional(readOnly = true)
    public Optional<LoyaltyProgram> getActiveProgram(UUID tenantId) {
        return programRepository.findFirstByTenantIdAndEnabledTrue(tenantId);
    }
}
