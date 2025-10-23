package com.minishop.reviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReplyRequest {

    @NotBlank(message = "Reply content is required")
    @Size(max = 1000, message = "Reply content must not exceed 1000 characters")
    private String content;
}
