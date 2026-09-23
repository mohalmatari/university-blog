package com.unversity.blog.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequestDTO {

    @NotBlank(message = "Comment content is required")
    private String content;
}