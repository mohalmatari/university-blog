package com.unversity.blog.demo.service;

import com.unversity.blog.demo.config.SecurityUtils;
import com.unversity.blog.demo.dto.CommentRequestDTO;
import com.unversity.blog.demo.dto.CommentResponseDTO;
import com.unversity.blog.demo.exception.ResourceNotFoundException;
import com.unversity.blog.demo.model.Comment;
import com.unversity.blog.demo.model.Post;
import com.unversity.blog.demo.model.User;
import com.unversity.blog.demo.repository.CommentRepository;
import com.unversity.blog.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public CommentResponseDTO createComment(Long postId, CommentRequestDTO requestDTO) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        User author = securityUtils.getCurrentUser();

        Comment comment = new Comment();
        comment.setContent(requestDTO.getContent());
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        return new CommentResponseDTO(savedComment);
    }

    public Page<CommentResponseDTO> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable)
                .map(CommentResponseDTO::new);
    }

    public List<CommentResponseDTO> getCommentsByUser(Long userId) {
        return commentRepository.findByAuthorId(userId)
                .stream()
                .map(CommentResponseDTO::new)
                .collect(Collectors.toList());
    }

    public CommentResponseDTO getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
        return new CommentResponseDTO(comment);
    }

    @Transactional
    public String deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        User currentUser = securityUtils.getCurrentUser();

        boolean isOwner = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to delete this comment");
        }

        commentRepository.deleteById(id);
        return "Comment with id " + id + " was deleted successfully";
    }
}