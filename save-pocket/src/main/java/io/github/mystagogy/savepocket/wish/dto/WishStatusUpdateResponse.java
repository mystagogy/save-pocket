package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.WishStatus;

public record WishStatusUpdateResponse(
        Long id,
        WishStatus status
) {
}
