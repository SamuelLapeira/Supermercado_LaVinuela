package com.lavinuela.shop.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserConsentJpaRepository extends JpaRepository<UserConsentJpaEntity, Long> {
}
