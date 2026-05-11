package io.github.mystagogy.savepocket.wish.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import io.github.mystagogy.savepocket.wish.dto.WishCreateRequest;
import io.github.mystagogy.savepocket.wish.dto.WishCreateResponse;
import io.github.mystagogy.savepocket.wish.dto.WishSearchItemResponse;
import io.github.mystagogy.savepocket.wish.dto.WishSummaryResponse;
import io.github.mystagogy.savepocket.wish.entity.DealSourceType;
import io.github.mystagogy.savepocket.wish.entity.ProductWish;
import io.github.mystagogy.savepocket.wish.entity.WishEventHistory;
import io.github.mystagogy.savepocket.wish.entity.WishEventType;
import io.github.mystagogy.savepocket.wish.entity.WishStatus;
import io.github.mystagogy.savepocket.wish.external.naver.NaverShoppingProduct;
import io.github.mystagogy.savepocket.wish.external.naver.NaverShoppingSearchClient;
import io.github.mystagogy.savepocket.wish.repository.PriceHistoryRepository;
import io.github.mystagogy.savepocket.wish.repository.ProductWishRepository;
import io.github.mystagogy.savepocket.wish.repository.WishEventHistoryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    private ProductWishRepository productWishRepository;

    @Mock
    private WishEventHistoryRepository wishEventHistoryRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NaverShoppingSearchClient naverShoppingSearchClient;

    private WishService wishService;

    @BeforeEach
    void setUp() {
        wishService = new WishService(
                productWishRepository,
                wishEventHistoryRepository,
                priceHistoryRepository,
                userRepository,
                naverShoppingSearchClient
        );
    }

    // 위시 등록 시 전달된 선택 항목 정보를 기준으로 WAITING 상태를 저장하고 REGISTERED 이벤트를 생성해야 한다.
    @Test
    void createWishInitializesStateAndCreatesRegisteredEvent() {
        User user = createUser(1L, "user@example.com");
        WishCreateRequest request = new WishCreateRequest(
                "https://shopping.naver.com/item/123",
                "3일 고민",
                "나이키 운동화",
                "https://img.naver.com/product.jpg",
                115000L,
                119000L,
                "https://instagram.com/sample",
                DealSourceType.INFLUENCER
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productWishRepository.save(any(ProductWish.class))).thenAnswer(invocation -> {
            ProductWish wish = invocation.getArgument(0);
            ReflectionTestUtils.setField(wish, "id", 10L);
            return wish;
        });

        WishCreateResponse response = wishService.createWish(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(WishStatus.WAITING);
        assertThat(response.name()).isEqualTo("나이키 운동화");
        assertThat(response.effectivePrice()).isEqualTo(119000L);
        assertThat(response.referencePrice()).isEqualTo(115000L);
        assertThat(Duration.between(response.lastViewedAt(), response.expireAt()).toHours()).isEqualTo(72L);

        ArgumentCaptor<WishEventHistory> eventCaptor = ArgumentCaptor.forClass(WishEventHistory.class);
        verify(wishEventHistoryRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(WishEventType.REGISTERED);
    }

    // 검색어로 조회하면 네이버 검색 결과 목록을 응답 DTO 리스트로 반환해야 한다.
    @Test
    void searchWishesReturnsMappedSearchItems() {
        when(naverShoppingSearchClient.searchProducts("에어팟")).thenReturn(List.of(
                new NaverShoppingProduct("에어팟 프로", "https://shopping.naver.com/item/1", "https://img/1.jpg", 299000L, "스마트스토어A"),
                new NaverShoppingProduct("에어팟 맥스", "https://shopping.naver.com/item/2", "https://img/2.jpg", 699000L, "스마트스토어B")
        ));

        List<WishSearchItemResponse> response = wishService.searchWishes("에어팟");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).name()).isEqualTo("에어팟 프로");
        assertThat(response.get(0).referencePrice()).isEqualTo(299000L);
        assertThat(response.get(0).mallName()).isEqualTo("스마트스토어A");
    }

    // 빈 검색어로 조회하면 유효성 예외를 반환해야 한다.
    @Test
    void searchWishesThrowsValidationWhenQueryIsBlank() {
        assertThatThrownBy(() -> wishService.searchWishes("  "))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    // 타 사용자 위시 상세 조회 요청이면 403 예외를 반환해야 한다.
    @Test
    void getWishDetailThrowsForbiddenWhenWishBelongsToAnotherUser() {
        when(productWishRepository.findByIdAndUser_Id(99L, 1L)).thenReturn(Optional.empty());
        when(productWishRepository.existsById(99L)).thenReturn(true);

        assertThatThrownBy(() -> wishService.getWishDetail(1L, 99L))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN_RESOURCE);
    }

    // 존재하지 않는 위시 상세 조회 요청이면 404 예외를 반환해야 한다.
    @Test
    void getWishDetailThrowsNotFoundWhenWishDoesNotExist() {
        when(productWishRepository.findByIdAndUser_Id(999L, 1L)).thenReturn(Optional.empty());
        when(productWishRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> wishService.getWishDetail(1L, 999L))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.WISH_NOT_FOUND);
    }

    // 상태 필터로 목록 조회하면 해당 상태의 위시들만 응답 DTO로 반환해야 한다.
    @Test
    void getWishesReturnsMappedSummaryByStatus() {
        ProductWish waitingWish = new ProductWish();
        waitingWish.setProductName("아이패드");
        waitingWish.setStatus(WishStatus.WAITING);
        waitingWish.setExpireAt(LocalDateTime.now().plusHours(72));
        waitingWish.setReferencePrice(1000000L);
        ReflectionTestUtils.setField(waitingWish, "id", 3L);

        when(productWishRepository.findByUser_IdAndStatusOrderByUpdatedAtDesc(1L, WishStatus.WAITING))
                .thenReturn(List.of(waitingWish));

        List<WishSummaryResponse> responses = wishService.getWishes(1L, WishStatus.WAITING);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(3L);
        assertThat(responses.get(0).name()).isEqualTo("아이패드");
        assertThat(responses.get(0).status()).isEqualTo(WishStatus.WAITING);
    }

    private User createUser(Long id, String email) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail(email);
        user.setPasswordHash("encoded");
        user.setNickname("tester");
        return user;
    }
}
