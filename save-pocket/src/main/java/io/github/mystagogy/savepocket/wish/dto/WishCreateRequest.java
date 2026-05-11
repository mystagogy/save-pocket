package io.github.mystagogy.savepocket.wish.dto;

import io.github.mystagogy.savepocket.wish.entity.DealSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record WishCreateRequest(
        @NotBlank(message = "상품 URL은 필수입니다.")
        String productUrl,

        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String memo,

        @NotBlank(message = "상품명은 필수입니다.")
        String productName,
        String productImageUrl,

        @PositiveOrZero(message = "기준 가격은 0 이상이어야 합니다.")
        Long referencePrice,

        @PositiveOrZero(message = "체감 가격은 0 이상이어야 합니다.")
        Long userDealPrice,

        String dealUrl,
        DealSourceType dealSourceType
) {
}
