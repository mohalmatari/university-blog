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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CommentService commentService;


    private User user(Long id, String username, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    private Post postWithId(Long id) {
        Post p = new Post();
        p.setId(id);
        return p;
    }

    private Comment commentOwnedBy(Long commentId, User author, Post post) {
        Comment c = new Comment();
        c.setId(commentId);
        c.setContent("Old comment");
        c.setAuthor(author);
        c.setPost(post);
        return c;
    }

    private CommentRequestDTO request(String content) {
        CommentRequestDTO dto = new CommentRequestDTO();
        dto.setContent(content);
        return dto;
    }

    // createComment

    @Test
    @DisplayName("createComment saves the comment with the logged-in user as author")
    void createComment_setsCurrentUserAsAuthor() {
        User author = user(1L, "mohammed", User.Role.USER);
        Post post = postWithId(5L);
        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        when(securityUtils.getCurrentUser()).thenReturn(author);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponseDTO result = commentService.createComment(5L, request("Nice post"));

        assertEquals("Nice post", result.getContent());
        assertEquals("mohammed", result.getAuthorUsername());
        assertEquals(5L, result.getPostId());
    }

    @Test
    @DisplayName("createComment throws ResourceNotFoundException when the post is missing")
    void createComment_whenPostIsMissing_throwsNotFound() {
        when(postRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.createComment(5L, request("Nice post")));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    // getComment
    @Test
    @DisplayName("getCommentById returns the comment when it exists")
    void getCommentById_whenExists_returnsDto() {
        User author = user(1L, "mohammed", User.Role.USER);
        Comment existing = commentOwnedBy(10L, author, postWithId(5L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(existing));

        CommentResponseDTO result = commentService.getCommentById(10L);

        assertEquals("Old comment", result.getContent());
        assertEquals("mohammed", result.getAuthorUsername());
    }

    @Test
    @DisplayName("getCommentById throws ResourceNotFoundException when the comment does not exist")
    void getCommentById_whenMissing_throwsNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentById(99L));
    }

    @Test
    @DisplayName("getCommentsByPost maps the page of comments to DTOs")
    void getCommentsByPost_returnsMappedPage() {
        User author = user(1L, "mohammed", User.Role.USER);
        Comment existing = commentOwnedBy(10L, author, postWithId(5L));
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findByPostId(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(existing), pageable, 1));

        Page<CommentResponseDTO> result = commentService.getCommentsByPost(5L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Old comment", result.getContent().get(0).getContent());
    }

    @Test
    @DisplayName("getCommentsByUser maps the user's comments to DTOs")
    void getCommentsByUser_returnsMappedList() {
        User author = user(1L, "mohammed", User.Role.USER);
        Comment existing = commentOwnedBy(10L, author, postWithId(5L));
        when(commentRepository.findByAuthorId(1L)).thenReturn(List.of(existing));

        List<CommentResponseDTO> result = commentService.getCommentsByUser(1L);

        assertEquals(1, result.size());
        assertEquals("mohammed", result.get(0).getAuthorUsername());
    }

    // deleteComment

    @Test
    @DisplayName("deleteComment lets the owner delete their own comment")
    void deleteComment_byOwner_deletes() {
        User owner = user(1L, "mohammed", User.Role.USER);
        Comment existing = commentOwnedBy(10L, owner, postWithId(5L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(securityUtils.getCurrentUser()).thenReturn(owner);

        String message = commentService.deleteComment(10L);

        verify(commentRepository).deleteById(10L);
        assertEquals("Comment with id 10 was deleted successfully", message);
    }

    @Test
    @DisplayName("deleteComment lets an admin delete someone else's comment")
    void deleteComment_byAdmin_deletes() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User admin = user(3L, "admin", User.Role.ADMIN);
        Comment existing = commentOwnedBy(10L, owner, postWithId(5L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(securityUtils.getCurrentUser()).thenReturn(admin);

        commentService.deleteComment(10L);

        verify(commentRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deleteComment rejects a user who is neither the owner nor an admin")
    void deleteComment_byOtherUser_throwsAccessDenied() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User stranger = user(2L, "someone_else", User.Role.USER);
        Comment existing = commentOwnedBy(10L, owner, postWithId(5L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(securityUtils.getCurrentUser()).thenReturn(stranger);

        assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(10L));

        // The most important check: nothing was deleted.
        verify(commentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteComment throws ResourceNotFoundException when the comment does not exist")
    void deleteComment_whenMissing_throwsNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.deleteComment(99L));

        verify(commentRepository, never()).deleteById(any());
    }
}