package io.github.mystagogy.savepocket.wish.dto;

public record WishSearchItemResponse(
        String name,
        String url,
        String imageUrl,
        Long referencePrice,
        String mallName
) {
}
