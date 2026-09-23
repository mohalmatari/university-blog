package com.unversity.blog.demo.controller;

import com.unversity.blog.demo.dto.CommentRequestDTO;
import com.unversity.blog.demo.dto.CommentResponseDTO;
import com.unversity.blog.demo.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public CommentResponseDTO createComment(
            @RequestParam Long postId,
            @Valid @RequestBody CommentRequestDTO requestDTO) {
        return commentService.createComment(postId, requestDTO);
    }

    @GetMapping("/post/{postId}")
    public Page<CommentResponseDTO> getCommentsByPost(
            @PathVariable Long postId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return commentService.getCommentsByPost(postId, pageable);
    }

    @GetMapping("/user/{userId}")
    public List<CommentResponseDTO> getCommentsByUser(@PathVariable Long userId) {
        return commentService.getCommentsByUser(userId);
    }

    @GetMapping("/{id}")
    public CommentResponseDTO getCommentById(@PathVariable Long id) {
        return commentService.getCommentById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteComment(@PathVariable Long id) {
        return commentService.deleteComment(id);
    }
}