package io.github.mystagogy.savepocket.wish.dto;

public record WishSearchItemResponse(
        String name,
        String url,
        String productId,
        String imageUrl,
        Long referencePrice,
        String mallName
) {
}
