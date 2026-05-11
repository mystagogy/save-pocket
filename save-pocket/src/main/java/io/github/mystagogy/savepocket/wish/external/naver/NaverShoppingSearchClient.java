package io.github.mystagogy.savepocket.wish.external.naver;

import java.util.List;

public interface NaverShoppingSearchClient {
    List<NaverShoppingProduct> searchProducts(String query);
}
