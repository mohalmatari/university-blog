package com.unversity.blog.demo.service;

import com.unversity.blog.demo.config.SecurityUtils;
import com.unversity.blog.demo.dto.PostRequestDTO;
import com.unversity.blog.demo.dto.PostResponseDTO;
import com.unversity.blog.demo.exception.ResourceNotFoundException;
import com.unversity.blog.demo.model.Post;
import com.unversity.blog.demo.model.User;
import com.unversity.blog.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public PostResponseDTO createPost(PostRequestDTO requestDTO) {
        User author = securityUtils.getCurrentUser();

        Post post = new Post();
        post.setTitle(requestDTO.getTitle());
        post.setContent(requestDTO.getContent());
        post.setAuthor(author);
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return new PostResponseDTO(savedPost);
    }

    public Page<PostResponseDTO> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(PostResponseDTO::new);
    }

    public PostResponseDTO getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return new PostResponseDTO(post);
    }

    @Transactional
    public PostResponseDTO updatePost(Long id, PostRequestDTO requestDTO) {
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        User currentUser = securityUtils.getCurrentUser();

        boolean isOwner = existingPost.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to edit this post");
        }

        existingPost.setTitle(requestDTO.getTitle());
        existingPost.setContent(requestDTO.getContent());
        existingPost.setUpdatedAt(LocalDateTime.now());

        Post updatedPost = postRepository.save(existingPost);
        return new PostResponseDTO(updatedPost);
    }

    @Transactional
    public String deletePost(Long id){
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        User currentUser = securityUtils.getCurrentUser();

        boolean isOwner = existingPost.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to delete this post");
        }

        postRepository.deleteById(id);
        return "Post with id " + id + " was deleted successfully";
    }
}