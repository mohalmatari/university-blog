package com.unversity.blog.demo.controller;

import com.unversity.blog.demo.dto.PostRequestDTO;
import com.unversity.blog.demo.dto.PostResponseDTO;
import com.unversity.blog.demo.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping
    public PostResponseDTO createPost(@Valid @RequestBody PostRequestDTO requestDTO) {
        return postService.createPost(requestDTO);
    }

    @GetMapping
    public Page<PostResponseDTO> getAllPosts(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return postService.getAllPosts(pageable);
    }

    @GetMapping("/{id}")
    public PostResponseDTO getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PutMapping("/{id}")
    public PostResponseDTO updatePost(@PathVariable Long id, @Valid @RequestBody PostRequestDTO requestDTO) {
        return postService.updatePost(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable Long id) {
        return postService.deletePost(id);
    }
}