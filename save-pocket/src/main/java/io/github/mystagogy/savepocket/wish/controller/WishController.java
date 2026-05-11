package io.github.mystagogy.savepocket.wish.controller;

import io.github.mystagogy.savepocket.common.response.ApiResponse;
import io.github.mystagogy.savepocket.common.security.CurrentUserProvider;
import io.github.mystagogy.savepocket.wish.dto.WishCreateRequest;
import io.github.mystagogy.savepocket.wish.dto.WishCreateResponse;
import io.github.mystagogy.savepocket.wish.dto.WishDetailResponse;
import io.github.mystagogy.savepocket.wish.dto.WishSearchItemResponse;
import io.github.mystagogy.savepocket.wish.dto.WishSummaryResponse;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.service.WishService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wishes")
public class WishController {

    private final WishService wishService;
    private final CurrentUserProvider currentUserProvider;

    public WishController(WishService wishService, CurrentUserProvider currentUserProvider) {
        this.wishService = wishService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WishCreateResponse>> createWish(@Valid @RequestBody WishCreateRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        WishCreateResponse response = wishService.createWish(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<WishSearchItemResponse>>> searchWishes(
            @RequestParam(required = false) String query
    ) {
        List<WishSearchItemResponse> response = wishService.searchWishes(query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishSummaryResponse>>> getWishes(
            @RequestParam(required = false) WishStatus status
    ) {
        Long userId = currentUserProvider.getCurrentUserId();
        List<WishSummaryResponse> response = wishService.getWishes(userId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WishDetailResponse>> getWishDetail(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        WishDetailResponse response = wishService.getWishDetail(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
