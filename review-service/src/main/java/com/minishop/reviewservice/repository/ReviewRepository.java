package com.minishop.reviewservice.repository;

import com.minishop.reviewservice.document.Review;
import com.minishop.reviewservice.document.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String>, ReviewRepositoryCustom {

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndRatingAndStatus(UUID productId, Integer rating, ReviewStatus status, Pageable pageable);

    boolean existsByOrderItemId(UUID orderItemId);

    Optional<Review> findByOrderItemId(UUID orderItemId);
}
