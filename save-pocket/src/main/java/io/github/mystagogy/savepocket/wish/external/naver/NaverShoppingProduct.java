package io.github.mystagogy.savepocket.wish.external.naver;

public record NaverShoppingProduct(
        String title,
        String link,
        String image,
        Long lowestPrice,
        String mallName
) {
}
