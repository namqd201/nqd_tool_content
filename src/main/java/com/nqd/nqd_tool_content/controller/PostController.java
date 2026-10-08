package com.nqd.nqd_tool_content.controller;

import com.nqd.nqd_tool_content.dto.request.BulkPostActionRequest;
import com.nqd.nqd_tool_content.dto.request.MarkPublishedRequest;
import com.nqd.nqd_tool_content.dto.request.UpdatePostRequest;
import com.nqd.nqd_tool_content.dto.response.ApiResponse;
import com.nqd.nqd_tool_content.dto.response.PostResponse;
import com.nqd.nqd_tool_content.dto.response.ScheduleSummaryResponse;
import com.nqd.nqd_tool_content.entity.PostAttempt;
import com.nqd.nqd_tool_content.entity.User;
import com.nqd.nqd_tool_content.service.PostService;
import com.nqd.nqd_tool_content.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final com.nqd.nqd_tool_content.repository.UserRepository userRepository;

    private UUID getEffectiveUserId() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa có người dùng");
        }
        return users.get(0).getId();
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<PostResponse>>> listPosts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) UUID planId,
            @RequestParam(required = false) UUID accountId
    ) {
        UUID userId = getEffectiveUserId();
        List<PostResponse> posts = postService.listPosts(userId, status, platform, planId, accountId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(userId, id)));
    }

    @PatchMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable UUID id,
            @RequestBody UpdatePostRequest req
    ) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.updatePost(userId, id, req)));
    }

    @PostMapping("/posts/{id}/publish-now")
    public ResponseEntity<ApiResponse<PostResponse>> publishNow(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.publishNow(userId, id)));
    }

    @PostMapping({"/posts/{id}/regenerate", "/posts/{id}/regenerate-content"})
    public ResponseEntity<ApiResponse<PostResponse>> regenerateContent(
            @PathVariable UUID id,
            @RequestBody(required = false) com.nqd.nqd_tool_content.dto.request.RegeneratePostRequest req
    ) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.regenerateContent(userId, id, req)));
    }

    @PostMapping("/posts/{id}/chat")
    public ResponseEntity<ApiResponse<com.nqd.nqd_tool_content.dto.response.PostChatResponse>> chatWithAI(
            @PathVariable UUID id,
            @Valid @RequestBody com.nqd.nqd_tool_content.dto.request.PostChatRequest req
    ) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.chatWithAI(userId, id, req)));
    }

    @PostMapping({"/posts/{id}/image/regenerate", "/posts/{id}/regenerate-image"})
    public ResponseEntity<ApiResponse<PostResponse>> regenerateImage(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.regenerateImage(userId, id)));
    }

    @PostMapping("/posts/{id}/image")
    public ResponseEntity<ApiResponse<PostResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) throws java.io.IOException {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.uploadCustomImage(
                userId, id, file.getBytes(), file.getContentType(), file.getOriginalFilename()
        )));
    }

    @DeleteMapping("/posts/{id}/image")
    public ResponseEntity<ApiResponse<PostResponse>> removeImage(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.removeImage(userId, id)));
    }

    @PostMapping("/posts/{id}/skip")
    public ResponseEntity<ApiResponse<PostResponse>> skipPost(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.skipPost(userId, id)));
    }

    @PostMapping("/posts/{id}/retry")
    public ResponseEntity<ApiResponse<PostResponse>> retryPost(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.retryPost(userId, id)));
    }

    @PostMapping("/posts/{id}/mark-published")
    public ResponseEntity<ApiResponse<PostResponse>> markPublished(
            @PathVariable UUID id,
            @Valid @RequestBody MarkPublishedRequest req
    ) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.markPublished(userId, id, req)));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        postService.deletePost(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/posts/bulk")
    public ResponseEntity<ApiResponse<Void>> bulkAction(@Valid @RequestBody BulkPostActionRequest req) {
        UUID userId = getEffectiveUserId();
        postService.bulkAction(userId, req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/posts/{id}/attempts")
    public ResponseEntity<ApiResponse<List<PostAttempt>>> getAttempts(@PathVariable UUID id) {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.getAttempts(userId, id)));
    }

    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getSchedule(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        UUID userId = getEffectiveUserId();
        List<PostResponse> posts = postService.listPosts(userId, null, null, null, null);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/schedule/summary")
    public ResponseEntity<ApiResponse<ScheduleSummaryResponse>> getScheduleSummary() {
        UUID userId = getEffectiveUserId();
        return ResponseEntity.ok(ApiResponse.success(postService.getScheduleSummary(userId)));
    }
}
