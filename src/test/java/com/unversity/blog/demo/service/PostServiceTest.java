package com.unversity.blog.demo.service;

import com.unversity.blog.demo.config.SecurityUtils;
import com.unversity.blog.demo.dto.PostRequestDTO;
import com.unversity.blog.demo.dto.PostResponseDTO;
import com.unversity.blog.demo.exception.ResourceNotFoundException;
import com.unversity.blog.demo.model.Post;
import com.unversity.blog.demo.model.User;
import com.unversity.blog.demo.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private PostService postService;


    private User user(Long id, String username, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    private Post postOwnedBy(Long postId, User author) {
        Post p = new Post();
        p.setId(postId);
        p.setTitle("Old title");
        p.setContent("Old content");
        p.setAuthor(author);
        return p;
    }

    private PostRequestDTO request(String title, String content) {
        PostRequestDTO dto = new PostRequestDTO();
        dto.setTitle(title);
        dto.setContent(content);
        return dto;
    }

    // Tests

    @Test
    @DisplayName("createPost saves the post with the logged-in user as author")
    void createPost_setsCurrentUserAsAuthor() {

        User author = user(1L, "mohammed", User.Role.USER);
        when(securityUtils.getCurrentUser()).thenReturn(author);

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponseDTO result = postService.createPost(request("Hello world", "First post"));

        assertEquals("Hello world", result.getTitle());
        assertEquals("First post", result.getContent());
        assertEquals("mohammed", result.getAuthorUsername());
    }

    @Test
    @DisplayName("getPostById throws ResourceNotFoundException when the post does not exist")
    void getPostById_whenMissing_throwsNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(99L));
    }

    @Test
    @DisplayName("updatePost lets the owner edit their own post")
    void updatePost_byOwner_succeeds() {
        User owner = user(1L, "mohammed", User.Role.USER);
        Post existing = postOwnedBy(10L, owner);
        when(postRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponseDTO result = postService.updatePost(10L, request("New title", "New content"));

        assertEquals("New title", result.getTitle());
        assertEquals("New content", result.getContent());
    }

    @Test
    @DisplayName("updatePost rejects a user who is neither the owner nor an admin")
    void updatePost_byOtherUser_throwsAccessDenied() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User stranger = user(2L, "someone_else", User.Role.USER);
        Post existing = postOwnedBy(10L, owner);
        when(postRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(securityUtils.getCurrentUser()).thenReturn(stranger);

        assertThrows(AccessDeniedException.class,
                () -> postService.updatePost(10L, request("Hacked", "Hacked")));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("updatePost lets an admin edit someone else's post")
    void updatePost_byAdmin_succeeds() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User admin = user(3L, "admin", User.Role.ADMIN);
        Post existing = postOwnedBy(10L, owner);
        when(postRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(securityUtils.getCurrentUser()).thenReturn(admin);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponseDTO result = postService.updatePost(10L, request("Moderated", "Edited by admin"));

        assertEquals("Moderated", result.getTitle());
    }

    @Test
    @DisplayName("deletePost removes the post when the owner asks")
    void deletePost_byOwner_deletes() {
        User owner = user(1L, "mohammed", User.Role.USER);
        when(postRepository.findById(10L)).thenReturn(Optional.of(postOwnedBy(10L, owner)));
        when(securityUtils.getCurrentUser()).thenReturn(owner);

        postService.deletePost(10L);

        verify(postRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deletePost rejects a user who is neither the owner nor an admin")
    void deletePost_byOtherUser_throwsAccessDenied() {
        User owner = user(1L, "mohammed", User.Role.USER);
        User stranger = user(2L, "someone_else", User.Role.USER);
        when(postRepository.findById(10L)).thenReturn(Optional.of(postOwnedBy(10L, owner)));
        when(securityUtils.getCurrentUser()).thenReturn(stranger);

        assertThrows(AccessDeniedException.class, () -> postService.deletePost(10L));

        verify(postRepository, never()).deleteById(any());
    }
}