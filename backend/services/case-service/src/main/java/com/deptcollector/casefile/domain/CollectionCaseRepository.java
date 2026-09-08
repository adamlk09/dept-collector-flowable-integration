package com.deptcollector.casefile.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionCaseRepository extends JpaRepository<CollectionCase, UUID> {

    Optional<CollectionCase> findByTenantIdAndCaseReference(String tenantId, String caseReference);

    List<CollectionCase> findByTenantId(String tenantId);
}
