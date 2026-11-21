package com.loyalty.customer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerStatsRepository extends JpaRepository<CustomerStats, UUID> {
}
