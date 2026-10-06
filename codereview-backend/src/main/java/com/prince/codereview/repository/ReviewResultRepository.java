package com.prince.codereview.repository;

import com.prince.codereview.model.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {
}