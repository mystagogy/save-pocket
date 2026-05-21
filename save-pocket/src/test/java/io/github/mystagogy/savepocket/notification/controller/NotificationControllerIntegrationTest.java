package io.github.mystagogy.savepocket.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.notification.entity.Notification;
import io.github.mystagogy.savepocket.notification.entity.NotificationType;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductWishRepository productWishRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WishEventHistoryRepository wishEventHistoryRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        wishEventHistoryRepository.deleteAll();
        priceHistoryRepository.deleteAll();
        productWishRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setNickname("유저");
        user = userRepository.save(user);
    }

    @Test
    void getNotificationsReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void streamNotificationsReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/notifications/stream"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getNotificationsReturnsListAndUnreadCount() throws Exception {
        ProductWish wish = saveWish(user, "테스트 상품");
        saveNotification(user, wish, false);
        saveNotification(user, wish, true);
        MockHttpSession session = loginSession("user@example.com", "Password123!");

        mockMvc.perform(get("/notifications").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void markNotificationAsReadUpdatesStatus() throws Exception {
        ProductWish wish = saveWish(user, "읽음 처리 상품");
        Notification notification = saveNotification(user, wish, false);
        MockHttpSession session = loginSession("user@example.com", "Password123!");

        mockMvc.perform(post("/notifications/{id}/read", notification.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(notification.getId()))
                .andExpect(jsonPath("$.data.read").value(true));
    }

    private MockHttpSession loginSession(String email, String password) throws Exception {
        String loginRequest = objectMapper.writeValueAsString(Map.of("email", email, "password", password));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private ProductWish saveWish(User owner, String name) {
        ProductWish wish = new ProductWish();
        wish.setUser(owner);
        wish.setProductName(name);
        wish.setProductUrl("https://shopping.naver.com/item/" + name.hashCode());
        wish.setStatus(WishStatus.WAITING);
        wish.setFirstRegisteredAt(LocalDateTime.now());
        wish.setLastViewedAt(LocalDateTime.now());
        wish.setExpireAt(LocalDateTime.now().plusHours(72));
        wish.setReactivatedCount(0);
        wish.setReferencePrice(10000L);
        return productWishRepository.save(wish);
    }

    private Notification saveNotification(User owner, ProductWish wish, boolean isRead) {
        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setWish(wish);
        notification.setNotificationType(NotificationType.PRICE_DROP_LOWEST);
        notification.setTitle("최저가 갱신");
        notification.setMessage("가격이 내려갔어요.");
        notification.setLinkUrl("/wishes/" + wish.getId());
        if (isRead) {
            notification.markAsRead(LocalDateTime.now());
        }
        return notificationRepository.save(notification);
    }
}
