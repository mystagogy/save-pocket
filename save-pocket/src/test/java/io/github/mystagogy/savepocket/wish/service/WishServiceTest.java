package io.github.mystagogy.savepocket.wish.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mystagogy.savepocket.auth.entity.User;
import io.github.mystagogy.savepocket.auth.repository.UserRepository;
import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import io.github.mystagogy.savepocket.wish.dto.WishCreateRequest;
import io.github.mystagogy.savepocket.wish.dto.WishCreateResponse;
import io.github.mystagogy.savepocket.wish.dto.WishSearchItemResponse;
import io.github.mystagogy.savepocket.wish.dto.WishStatusUpdateResponse;
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

    // WAITING 상태 위시를 구매 처리하면 PURCHASED로 전환되고 이벤트가 기록되어야 한다.
    @Test
    void purchaseWishChangesStatusAndCreatesEvent() {
        ProductWish wish = new ProductWish();
        wish.setStatus(WishStatus.WAITING);
        ReflectionTestUtils.setField(wish, "id", 5L);

        when(productWishRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(wish));

        WishStatusUpdateResponse response = wishService.purchaseWish(1L, 5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(WishStatus.PURCHASED);
        assertThat(wish.getStatus()).isEqualTo(WishStatus.PURCHASED);

        ArgumentCaptor<WishEventHistory> eventCaptor = ArgumentCaptor.forClass(WishEventHistory.class);
        verify(wishEventHistoryRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(WishEventType.PURCHASED);
    }

    // 이미 구매 완료된 위시에 구매 요청하면 상태 전이 예외를 반환해야 한다.
    @Test
    void purchaseWishThrowsWhenStatusTransitionIsInvalid() {
        ProductWish wish = new ProductWish();
        wish.setStatus(WishStatus.PURCHASED);

        when(productWishRepository.findByIdAndUser_Id(5L, 1L)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> wishService.purchaseWish(1L, 5L))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_WISH_STATE);
    }

    // EXPIRED 상태 위시를 삭제 처리하면 DELETED로 전환되고 이벤트가 기록되어야 한다.
    @Test
    void deleteWishChangesStatusAndCreatesEvent() {
        ProductWish wish = new ProductWish();
        wish.setStatus(WishStatus.EXPIRED);
        ReflectionTestUtils.setField(wish, "id", 8L);

        when(productWishRepository.findByIdAndUser_Id(8L, 1L)).thenReturn(Optional.of(wish));

        WishStatusUpdateResponse response = wishService.deleteWish(1L, 8L);

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.status()).isEqualTo(WishStatus.DELETED);
        assertThat(wish.getStatus()).isEqualTo(WishStatus.DELETED);

        ArgumentCaptor<WishEventHistory> eventCaptor = ArgumentCaptor.forClass(WishEventHistory.class);
        verify(wishEventHistoryRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(WishEventType.DELETED);
    }

    // EXPIRED 상태 위시를 보류 재추가하면 WAITING으로 복귀하고 재활성화 정보가 갱신되어야 한다.
    @Test
    void reactivateWishRestoresWaitingStateAndUpdatesReactivationFields() {
        ProductWish wish = new ProductWish();
        wish.setStatus(WishStatus.EXPIRED);
        wish.setReactivatedCount(2);
        wish.setExpireAt(LocalDateTime.now().minusHours(1));
        wish.setExpiredAt(LocalDateTime.now().minusMinutes(30));
        wish.setSavedAmount(10000L);
        ReflectionTestUtils.setField(wish, "id", 13L);

        when(productWishRepository.findByIdAndUser_Id(13L, 1L)).thenReturn(Optional.of(wish));

        WishStatusUpdateResponse response = wishService.reactivateWish(1L, 13L);

        assertThat(response.id()).isEqualTo(13L);
        assertThat(response.status()).isEqualTo(WishStatus.WAITING);
        assertThat(wish.getStatus()).isEqualTo(WishStatus.WAITING);
        assertThat(wish.getReactivatedCount()).isEqualTo(3);
        assertThat(wish.getExpireAt()).isAfter(LocalDateTime.now().plusHours(71));
        assertThat(wish.getExpiredAt()).isNull();
        assertThat(wish.getSavedAmount()).isNull();

        ArgumentCaptor<WishEventHistory> eventCaptor = ArgumentCaptor.forClass(WishEventHistory.class);
        verify(wishEventHistoryRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(WishEventType.REACTIVATED);
    }

    // WAITING 상태 위시를 보류 재추가 요청하면 상태 전이 예외를 반환해야 한다.
    @Test
    void reactivateWishThrowsWhenStatusTransitionIsInvalid() {
        ProductWish wish = new ProductWish();
        wish.setStatus(WishStatus.WAITING);
        wish.setReactivatedCount(0);

        when(productWishRepository.findByIdAndUser_Id(15L, 1L)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> wishService.reactivateWish(1L, 15L))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_WISH_STATE);
    }

    // DELETED 상태 위시를 보류 재추가 요청하면 상태 전이 예외를 반환해야 한다.
    @Test
    void reactivateWishThrowsWhenWishIsDeleted() {
        ProductWish wish = new ProductWish();
        wish.setStatus(WishStatus.DELETED);
        wish.setReactivatedCount(1);

        when(productWishRepository.findByIdAndUser_Id(16L, 1L)).thenReturn(Optional.of(wish));

        assertThatThrownBy(() -> wishService.reactivateWish(1L, 16L))
                .isInstanceOf(SavePocketException.class)
                .extracting(ex -> ((SavePocketException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_WISH_STATE);
    }

    // 만료 시각이 지난 WAITING 위시는 EXPIRED로 전환하고 savedAmount/expiredAt/EXPIRED 이벤트를 기록해야 한다.
    @Test
    void expireDueWishesUpdatesStatusAndCreatesEvent() {
        LocalDateTime targetTime = LocalDateTime.of(2026, 5, 12, 18, 0);

        ProductWish wishWithDealPrice = new ProductWish();
        wishWithDealPrice.setStatus(WishStatus.WAITING);
        wishWithDealPrice.setReferencePrice(120000L);
        wishWithDealPrice.setUserDealPrice(110000L);
        ReflectionTestUtils.setField(wishWithDealPrice, "id", 21L);

        ProductWish wishWithReferencePrice = new ProductWish();
        wishWithReferencePrice.setStatus(WishStatus.WAITING);
        wishWithReferencePrice.setReferencePrice(90000L);
        ReflectionTestUtils.setField(wishWithReferencePrice, "id", 22L);

        when(productWishRepository.findByStatusAndExpireAtLessThanEqual(WishStatus.WAITING, targetTime))
                .thenReturn(List.of(wishWithDealPrice, wishWithReferencePrice));

        int expiredCount = wishService.expireDueWishes(targetTime);

        assertThat(expiredCount).isEqualTo(2);

        assertThat(wishWithDealPrice.getStatus()).isEqualTo(WishStatus.EXPIRED);
        assertThat(wishWithDealPrice.getExpiredAt()).isEqualTo(targetTime);
        assertThat(wishWithDealPrice.getSavedAmount()).isEqualTo(110000L);

        assertThat(wishWithReferencePrice.getStatus()).isEqualTo(WishStatus.EXPIRED);
        assertThat(wishWithReferencePrice.getExpiredAt()).isEqualTo(targetTime);
        assertThat(wishWithReferencePrice.getSavedAmount()).isEqualTo(90000L);

        ArgumentCaptor<WishEventHistory> eventCaptor = ArgumentCaptor.forClass(WishEventHistory.class);
        verify(wishEventHistoryRepository, times(2)).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .allSatisfy(event -> {
                    assertThat(event.getEventType()).isEqualTo(WishEventType.EXPIRED);
                    assertThat(event.getEventAt()).isEqualTo(targetTime);
                });
    }

    // 만료 대상이 없으면 상태 변경과 이벤트 저장 없이 0을 반환해야 한다.
    @Test
    void expireDueWishesReturnsZeroWhenNoTargetExists() {
        LocalDateTime targetTime = LocalDateTime.of(2026, 5, 12, 18, 0);
        when(productWishRepository.findByStatusAndExpireAtLessThanEqual(WishStatus.WAITING, targetTime))
                .thenReturn(List.of());

        int expiredCount = wishService.expireDueWishes(targetTime);

        assertThat(expiredCount).isZero();
        verify(wishEventHistoryRepository, never()).save(any(WishEventHistory.class));
        verify(productWishRepository).findByStatusAndExpireAtLessThanEqual(eq(WishStatus.WAITING), eq(targetTime));
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
