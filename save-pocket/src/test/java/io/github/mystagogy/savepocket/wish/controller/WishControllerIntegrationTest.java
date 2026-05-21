package io.github.mystagogy.savepocket.wish.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.notification.repository.NotificationRepository;
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.external.naver.NaverShoppingProduct;
import io.github.mystagogy.savepocket.wish.external.naver.NaverShoppingSearchClient;
import io.github.mystagogy.savepocket.wish.repository.PriceHistoryRepository;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import io.github.mystagogy.savepocket.wish.repository.WishEventHistoryRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductWishRepository productWishRepository;

    @Autowired
    private WishEventHistoryRepository wishEventHistoryRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private NaverShoppingSearchClient naverShoppingSearchClient;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        wishEventHistoryRepository.deleteAll();
        priceHistoryRepository.deleteAll();
        productWishRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setPasswordHash(passwordEncoder.encode("Password123!"));
        user1.setNickname("유저1");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash(passwordEncoder.encode("Password123!"));
        user2.setNickname("유저2");
        user2 = userRepository.save(user2);
    }

    // 인증 없이 위시 등록 요청 시 401을 반환해야 한다.
    @Test
    void createWishReturns401WhenUnauthenticated() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "productUrl", "https://smartstore.naver.com/sample/products/1",
                "productName", "에어팟"
        ));

        mockMvc.perform(post("/wishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // 인증된 사용자의 위시 등록 요청 시 201과 WAITING 상태 응답을 반환해야 한다.
    @Test
    void createWishReturns201WhenAuthenticated() throws Exception {
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "productUrl", "https://shopping.naver.com/item/10",
                "productName", "나이키 운동화",
                "memo", "3일 고민",
                "referencePrice", 119000,
                "userDealPrice", 119000
        ));

        mockMvc.perform(post("/wishes")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("나이키 운동화"))
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    // 검색어로 조회하면 네이버 검색 결과 리스트를 반환해야 한다.
    @Test
    void searchWishesReturnsNaverResultList() throws Exception {
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");
        when(naverShoppingSearchClient.searchProducts("에어팟")).thenReturn(java.util.List.of(
                new NaverShoppingProduct("에어팟 프로", "https://shopping.naver.com/item/1", "111", "https://img/1.jpg", 299000L, "몰A"),
                new NaverShoppingProduct("에어팟 맥스", "https://shopping.naver.com/item/2", "222", "https://img/2.jpg", 699000L, "몰B")
        ));

        mockMvc.perform(get("/wishes/search")
                        .session(session)
                        .param("query", "에어팟"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("에어팟 프로"))
                .andExpect(jsonPath("$.data[0].url").value("https://shopping.naver.com/item/1"));
    }

    // 검색어 파라미터 없이 조회하면 400 유효성 검증 실패를 반환해야 한다.
    @Test
    void searchWishesReturns400WhenQueryIsMissing() throws Exception {
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(get("/wishes/search")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // 목록 조회 시 로그인한 사용자 소유의 위시만 반환해야 한다.
    @Test
    void getWishesReturnsOnlyCurrentUsersWishes() throws Exception {
        saveWish(user1, "유저1 상품", WishStatus.WAITING);
        saveWish(user2, "유저2 상품", WishStatus.WAITING);

        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(get("/wishes")
                        .session(session)
                        .param("status", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("유저1 상품"));
    }

    // 타 사용자 위시 상세 조회 시 403을 반환해야 한다.
    @Test
    void getWishDetailReturns403ForOtherUsersWish() throws Exception {
        ProductWish otherUsersWish = saveWish(user2, "타인 상품", WishStatus.WAITING);
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(get("/wishes/{id}", otherUsersWish.getId())
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN_RESOURCE"));
    }

    // WAITING 상태 위시를 구매 요청하면 PURCHASED 상태로 변경되어야 한다.
    @Test
    void purchaseWishReturnsPurchasedStatus() throws Exception {
        ProductWish wish = saveWish(user1, "구매할 상품", WishStatus.WAITING);
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(post("/wishes/{id}/purchase", wish.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(wish.getId()))
                .andExpect(jsonPath("$.data.status").value("PURCHASED"));
    }

    // EXPIRED 상태 위시를 삭제 요청하면 DELETED 상태로 변경되어야 한다.
    @Test
    void deleteWishReturnsDeletedStatus() throws Exception {
        ProductWish wish = saveWish(user1, "삭제할 상품", WishStatus.EXPIRED);
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(post("/wishes/{id}/delete", wish.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(wish.getId()))
                .andExpect(jsonPath("$.data.status").value("DELETED"));
    }

    // 이미 구매된 위시에 삭제 요청하면 상태 전이 오류를 반환해야 한다.
    @Test
    void deleteWishReturns400WhenStatusTransitionIsInvalid() throws Exception {
        ProductWish wish = saveWish(user1, "이미 구매한 상품", WishStatus.PURCHASED);
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(post("/wishes/{id}/delete", wish.getId())
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_WISH_STATE"));
    }

    // EXPIRED 상태 위시를 보류 재추가 요청하면 WAITING 상태로 변경되어야 한다.
    @Test
    void reactivateWishReturnsWaitingStatus() throws Exception {
        ProductWish wish = saveWish(user1, "다시 추가할 상품", WishStatus.EXPIRED);
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(post("/wishes/{id}/reactivate", wish.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(wish.getId()))
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    // DELETED 상태 위시를 보류 재추가 요청하면 상태 전이 오류를 반환해야 한다.
    @Test
    void reactivateWishReturns400WhenWishIsDeleted() throws Exception {
        ProductWish wish = saveWish(user1, "삭제된 상품", WishStatus.DELETED);
        org.springframework.mock.web.MockHttpSession session = loginSession("user1@example.com", "Password123!");

        mockMvc.perform(post("/wishes/{id}/reactivate", wish.getId())
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_WISH_STATE"));
    }

    private org.springframework.mock.web.MockHttpSession loginSession(String email, String password) throws Exception {
        String loginRequest = objectMapper.writeValueAsString(Map.of("email", email, "password", password));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        return (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private ProductWish saveWish(User owner, String productName, WishStatus status) {
        ProductWish wish = new ProductWish();
        wish.setUser(owner);
        wish.setProductName(productName);
        wish.setProductUrl("https://smartstore.naver.com/sample/products/" + productName.hashCode());
        wish.setStatus(status);
        wish.setFirstRegisteredAt(LocalDateTime.now());
        wish.setLastViewedAt(LocalDateTime.now());
        wish.setExpireAt(LocalDateTime.now().plusHours(72));
        wish.setReactivatedCount(0);
        wish.setReferencePrice(10000L);
        return productWishRepository.save(wish);
    }
}
