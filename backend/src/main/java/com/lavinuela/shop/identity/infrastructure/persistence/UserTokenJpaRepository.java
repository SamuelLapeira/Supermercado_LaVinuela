package com.lavinuela.shop.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserTokenJpaRepository extends JpaRepository<UserTokenJpaEntity, Long> {

    Optional<UserTokenJpaEntity> findFirstByUserIdAndTokenTypeAndUsedAtIsNullOrderByIdDesc(long userId, String tokenType);

    Optional<UserTokenJpaEntity> findFirstByUserIdAndTokenTypeOrderByIdDesc(long userId, String tokenType);

    int countByUserIdAndTokenTypeAndCreatedAtAfter(long userId, String tokenType, Instant since);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserTokenJpaEntity t set t.usedAt = :now"
            + " where t.userId = :userId and t.tokenType = :type and t.usedAt is null")
    int invalidateActive(@Param("userId") long userId, @Param("type") String type, @Param("now") Instant now);
}
