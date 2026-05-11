package io.github.mystagogy.savepocket.wish.external.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.mystagogy.savepocket.config.NaverApiProperties;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NaverShoppingSearchHttpClient implements NaverShoppingSearchClient {

    private static final String HEADER_CLIENT_ID = "X-Naver-Client-Id";
    private static final String HEADER_CLIENT_SECRET = "X-Naver-Client-Secret";

    private final RestClient restClient;
    private final NaverApiProperties properties;

    public NaverShoppingSearchHttpClient(RestClient.Builder restClientBuilder, NaverApiProperties properties) {
        this.restClient = restClientBuilder.baseUrl("https://openapi.naver.com").build();
        this.properties = properties;
    }

    @Override
    public List<NaverShoppingProduct> searchProducts(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        if (!StringUtils.hasText(properties.clientId()) || !StringUtils.hasText(properties.clientSecret())) {
            return List.of();
        }

        NaverShopSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(properties.shoppingUrl())
                        .queryParam("query", query)
                        .queryParam("display", 20)
                        .queryParam("sort", "sim")
                        .build())
                .header(HEADER_CLIENT_ID, properties.clientId())
                .header(HEADER_CLIENT_SECRET, properties.clientSecret())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(NaverShopSearchResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        return response.items().stream()
                .map(item -> new NaverShoppingProduct(
                        normalizeText(item.title()),
                        item.link(),
                        item.image(),
                        parsePrice(item.lprice()),
                        item.mallName()
                ))
                .collect(Collectors.toList());
    }

    private static String normalizeText(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return rawText;
        }
        return rawText
                .replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .trim();
    }

    private static Long parsePrice(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverShopSearchResponse(List<NaverItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NaverItem(
            String title,
            String link,
            String image,
            String lprice,
            String mallName
    ) {
    }
}
