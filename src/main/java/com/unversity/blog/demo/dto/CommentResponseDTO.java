package com.unversity.blog.demo.dto;

import com.unversity.blog.demo.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String content;
    private String authorUsername;
    private Long postId;
    private LocalDateTime createdAt;

    public CommentResponseDTO(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.authorUsername = comment.getAuthor().getUsername();
        this.postId = comment.getPost().getId();
        this.createdAt = comment.getCreatedAt();
    }
}