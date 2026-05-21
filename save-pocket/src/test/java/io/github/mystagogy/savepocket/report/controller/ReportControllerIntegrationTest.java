package io.github.mystagogy.savepocket.report.controller;

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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

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
        user1.setEmail("report-user1@example.com");
        user1.setPasswordHash(passwordEncoder.encode("Password123!"));
        user1.setNickname("리포트유저1");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setEmail("report-user2@example.com");
        user2.setPasswordHash(passwordEncoder.encode("Password123!"));
        user2.setNickname("리포트유저2");
        user2 = userRepository.save(user2);
    }

    @Test
    void getMonthlyReportReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/reports/monthly"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMonthlyReportReturnsNetSavedAmount() throws Exception {
        saveExpiredWish(user1, "이번달 만료", 120000L);
        savePurchasedWish(user1, "이번달 구매", 30000L);
        saveExpiredWish(user2, "타유저 만료", 990000L);

        org.springframework.mock.web.MockHttpSession session = loginSession("report-user1@example.com", "Password123!");

        mockMvc.perform(get("/reports/monthly")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiredAmount").value(120000))
                .andExpect(jsonPath("$.data.purchasedAmount").value(30000))
                .andExpect(jsonPath("$.data.netSavedAmount").value(90000))
                .andExpect(jsonPath("$.data.details.length()").value(2))
                .andExpect(jsonPath("$.data.details[0].status").value("PURCHASED"))
                .andExpect(jsonPath("$.data.details[0].signedAmount").value(-30000))
                .andExpect(jsonPath("$.data.details[1].status").value("EXPIRED"))
                .andExpect(jsonPath("$.data.details[1].signedAmount").value(120000));
    }

    @Test
    void getMonthlyReportCountsExpiredAmountEvenAfterStatusChanged() throws Exception {
        saveExpiredThenPurchasedWish(user1, "만료후구매", 45000L, 45000L);

        org.springframework.mock.web.MockHttpSession session = loginSession("report-user1@example.com", "Password123!");

        mockMvc.perform(get("/reports/monthly")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiredAmount").value(45000))
                .andExpect(jsonPath("$.data.purchasedAmount").value(45000))
                .andExpect(jsonPath("$.data.netSavedAmount").value(0));
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

    private void saveExpiredWish(User owner, String productName, long savedAmount) {
        ProductWish wish = new ProductWish();
        wish.setUser(owner);
        wish.setProductName(productName);
        wish.setProductUrl("https://example.com/" + productName.hashCode());
        wish.setStatus(WishStatus.EXPIRED);
        wish.setFirstRegisteredAt(LocalDateTime.now().minusDays(3));
        wish.setLastViewedAt(LocalDateTime.now().minusDays(3));
        wish.setExpireAt(LocalDateTime.now().minusDays(1));
        wish.setExpiredAt(LocalDateTime.now().minusHours(2));
        wish.setReactivatedCount(0);
        wish.setReferencePrice(savedAmount);
        wish.setSavedAmount(savedAmount);
        productWishRepository.save(wish);
    }

    private void savePurchasedWish(User owner, String productName, long effectivePrice) {
        ProductWish wish = new ProductWish();
        wish.setUser(owner);
        wish.setProductName(productName);
        wish.setProductUrl("https://example.com/" + productName.hashCode());
        wish.setStatus(WishStatus.PURCHASED);
        wish.setFirstRegisteredAt(LocalDateTime.now().minusDays(2));
        wish.setLastViewedAt(LocalDateTime.now().minusDays(1));
        wish.setExpireAt(LocalDateTime.now().plusDays(1));
        wish.setReactivatedCount(0);
        wish.setReferencePrice(effectivePrice);
        productWishRepository.save(wish);
    }

    private void saveExpiredThenPurchasedWish(User owner, String productName, long savedAmount, long purchasedAmount) {
        ProductWish wish = new ProductWish();
        wish.setUser(owner);
        wish.setProductName(productName);
        wish.setProductUrl("https://example.com/" + productName.hashCode());
        wish.setStatus(WishStatus.PURCHASED);
        wish.setFirstRegisteredAt(LocalDateTime.now().minusDays(3));
        wish.setLastViewedAt(LocalDateTime.now().minusDays(2));
        wish.setExpireAt(LocalDateTime.now().minusDays(1));
        wish.setExpiredAt(LocalDateTime.now().minusHours(6));
        wish.setReactivatedCount(0);
        wish.setReferencePrice(purchasedAmount);
        wish.setSavedAmount(savedAmount);
        productWishRepository.save(wish);
    }
}
