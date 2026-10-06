package com.prince.codereview.repository;

import com.prince.codereview.model.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByReviewResultId(Long reviewResultId);
}