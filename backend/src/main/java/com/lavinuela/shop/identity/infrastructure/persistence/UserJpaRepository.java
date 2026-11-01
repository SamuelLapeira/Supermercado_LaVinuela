package com.lavinuela.shop.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByPublicId(String publicId);

    boolean existsByUsername(String username);

    @Query(value = "SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id = r.id WHERE ur.user_id = :userId ORDER BY r.code",
            nativeQuery = true)
    List<String> findRoleCodes(@Param("userId") long userId);

    /** Idempotente: si la cuenta ya tiene el rol, no hace nada. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO user_roles (user_id, role_id) SELECT :userId, r.id FROM roles r WHERE r.code = :roleCode",
            nativeQuery = true)
    int assignRole(@Param("userId") long userId, @Param("roleCode") String roleCode);
}
