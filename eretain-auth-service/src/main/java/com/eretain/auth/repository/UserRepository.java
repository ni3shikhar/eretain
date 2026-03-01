package com.eretain.auth.repository;

import com.eretain.auth.entity.User;
import com.eretain.common.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmployeeId(String employeeId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    Page<User> findByStatus(EmployeeStatus status, Pageable pageable);
    Page<User> findByActiveTrue(Pageable pageable);
}
