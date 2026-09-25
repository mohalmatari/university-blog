package com.unversity.blog.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDTO {

    @NotBlank(message = "Comment content is required")
    @Size(max = 500)
    private String content;
}