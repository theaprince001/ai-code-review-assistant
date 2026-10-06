package com.prince.codereview.repository;

import com.prince.codereview.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    Optional<Repository> findByFullName(String fullName);
    List<Repository> findByUserId(Long userId);
}