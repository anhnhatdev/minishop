package com.minishop.reviewservice.mapper;

import com.minishop.reviewservice.document.Review;
import com.minishop.reviewservice.dto.response.ReviewResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toReviewResponse(Review review);

    List<ReviewResponse> toReviewResponseList(List<Review> reviews);
}
