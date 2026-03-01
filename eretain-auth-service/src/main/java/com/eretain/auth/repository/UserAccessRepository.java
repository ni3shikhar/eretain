package com.eretain.auth.repository;

import com.eretain.auth.entity.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccessRepository extends JpaRepository<UserAccess, Long> {
    List<UserAccess> findByUserId(Long userId);
    Optional<UserAccess> findByUserIdAndModuleName(Long userId, String moduleName);
    void deleteByUserIdAndModuleName(Long userId, String moduleName);
}
