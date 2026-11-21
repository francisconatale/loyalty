package com.loyalty.automation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AutomationActionRepository extends JpaRepository<AutomationAction, UUID> {
}
