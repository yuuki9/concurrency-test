package com.yuuki.demo;

import com.yuuki.demo.domain.Coupon;
import com.yuuki.demo.dto.CouponIssueResponse;
import com.yuuki.demo.repository.CouponIssueRepository;
import com.yuuki.demo.repository.CouponRepository;
import com.yuuki.demo.service.CouponServiceSync;
import com.yuuki.demo.service.CouponServicePessimistic;
import com.yuuki.demo.service.CouponServiceOptimistic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class CouponConcurrencyTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponServiceSync couponServiceSync;

    @Autowired
    private CouponServicePessimistic couponServicePessimistic;

    @Autowired
    private CouponServiceOptimistic couponServiceOptimistic;

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // 테스트용 쿠폰 생성 (100개 한정)
        testCoupon = Coupon.builder()
                .name("선착순 100명 할인쿠폰")
                .totalQuantity(100)
                .build();
        couponRepository.save(testCoupon);
        log.info("테스트 쿠폰 생성 완료 - id: {}, quantity: {}", testCoupon.getId(), testCoupon.getTotalQuantity());
    }

    @AfterEach
    void after() {
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
        log.info("테스트 데이터 정리 완료");
    }
//
//    @Test
//    @DisplayName("쿠폰 발급")
//    void 쿠폰발급() {
//
//    }

    @Test
    @DisplayName("쿠폰 동시에 100개")
    void 쿠폰발급_동시에_100개() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.execute(() -> {
                try {
                    CouponIssueResponse response = couponServiceSync.issueCoupon(testCoupon.getId(), userId);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        
        // 모든 스레드가 완료될 때까지 대기
        countDownLatch.await();
        executorService.shutdown();

        Coupon fresh =
                couponRepository.findById(testCoupon.getId()).orElseThrow();
        log.info("남은 쿠폰 양 >>>> {}", fresh.getRemainingQuantity());
        log.info("발급된 쿠폰 양 >>>> {}", fresh.getIssuedQuantity());
        log.info("DB에 저장된 발급 이력 수 >>>> {}", couponIssueRepository.count());
        
        // 검증
        assertThat(fresh.getIssuedQuantity()).isEqualTo(100);
        assertThat(fresh.getRemainingQuantity()).isEqualTo(0);
        assertThat(couponIssueRepository.count()).isEqualTo(100);

    }
}

//    @Test
//    @DisplayName("Synchronized 1000명이 동시에 100개 쿠폰 발급 요청 (동시성 제어 실패 예상)")
//    void testSynchronized_ConcurrencyIssue() throws InterruptedException {
//        int threadCount = 1000;
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        long startTime = System.currentTimeMillis();
//
//        for (int i = 0; i < threadCount; i++) {
//            long userId = i;
//            executorService.submit(() -> {
//                try {
//                    CouponIssueResponse response = couponServiceSync.issueCoupon(testCoupon.getId(), userId);
//                    if (response.isSuccess()) {
//                        successCount.incrementAndGet();
//                    } else {
//                        failCount.incrementAndGet();
//                    }
//                } catch (Exception e) {
//                    log.error("쿠폰 발급 실패 - userId: {}, error: {}", userId, e.getMessage());
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        long endTime = System.currentTimeMillis();
//        long duration = endTime - startTime;
//
//        // 최종 쿠폰 상태 확인
//        Coupon finalCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
//
//        log.info("=== Synchronized 테스트 결과 ===");
//        log.info("총 요청 수: {}", threadCount);
//        log.info("성공 수: {}", successCount.get());
//        log.info("실패 수: {}", failCount.get());
//        log.info("최종 발급 수량: {}", finalCoupon.getIssuedQuantity());
//        log.info("DB에 저장된 발급 이력 수: {}", couponIssueRepository.count());
//        log.info("소요 시간: {}ms", duration);
//        log.info("===================================");
//
//    }
//}
//
//    @Test
//    @DisplayName("Pessimistic Lock: 1000명이 동시에 100개 쿠폰 발급 요청 (정합성 보장)")
//    void testPessimisticLock_Success() throws InterruptedException {
//        int threadCount = 1000;
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        long startTime = System.currentTimeMillis();
//
//        for (int i = 0; i < threadCount; i++) {
//            long userId = i;
//            executorService.submit(() -> {
//                try {
//                    CouponIssueResponse response = couponServicePessimistic.issueCoupon(
//                            testCoupon.getId(), userId);
//                    if (response.isSuccess()) {
//                        successCount.incrementAndGet();
//                    } else {
//                        failCount.incrementAndGet();
//                    }
//                } catch (Exception e) {
//                    log.error("쿠폰 발급 실패 - userId: {}, error: {}", userId, e.getMessage());
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        long endTime = System.currentTimeMillis();
//        long duration = endTime - startTime;
//
//        // 최종 쿠폰 상태 확인
//        Coupon finalCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
//
//        log.info("=== Pessimistic Lock 테스트 결과 ===");
//        log.info("총 요청 수: {}", threadCount);
//        log.info("성공 수: {}", successCount.get());
//        log.info("실패 수: {}", failCount.get());
//        log.info("최종 발급 수량: {}", finalCoupon.getIssuedQuantity());
//        log.info("DB에 저장된 발급 이력 수: {}", couponIssueRepository.count());
//        log.info("소요 시간: {}ms", duration);
//        log.info("======================================");
//
//        // 검증: 정확히 100개만 발급되어야 함
//        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(100);
//        assertThat(couponIssueRepository.count()).isEqualTo(100);
//        assertThat(successCount.get()).isEqualTo(100);
//        assertThat(failCount.get()).isEqualTo(900);
//
//        log.info("✅ 비관적 락으로 정합성이 보장되었습니다!");
//    }
//
//    @Test
//    @DisplayName("Optimistic Lock: 1000명이 동시에 100개 쿠폰 발급 요청 (정합성 보장)")
//    void testOptimisticLock_Success() throws InterruptedException {
//        int threadCount = 1000;
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failCount = new AtomicInteger(0);
//
//        long startTime = System.currentTimeMillis();
//
//        for (int i = 0; i < threadCount; i++) {
//            long userId = i;
//            executorService.submit(() -> {
//                try {
//                    CouponIssueResponse response = couponServiceOptimistic.issueCouponWithRetry(
//                            testCoupon.getId(), userId);
//                    if (response.isSuccess()) {
//                        successCount.incrementAndGet();
//                    } else {
//                        failCount.incrementAndGet();
//                    }
//                } catch (Exception e) {
//                    log.error("쿠폰 발급 실패 - userId: {}, error: {}", userId, e.getMessage());
//                    failCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        long endTime = System.currentTimeMillis();
//        long duration = endTime - startTime;
//
//        // 최종 쿠폰 상태 확인
//        Coupon finalCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
//
//        log.info("=== Optimistic Lock 테스트 결과 ===");
//        log.info("총 요청 수: {}", threadCount);
//        log.info("성공 수: {}", successCount.get());
//        log.info("실패 수: {}", failCount.get());
//        log.info("최종 발급 수량: {}", finalCoupon.getIssuedQuantity());
//        log.info("DB에 저장된 발급 이력 수: {}", couponIssueRepository.count());
//        log.info("소요 시간: {}ms", duration);
//        log.info("=====================================");
//
//        // 검증: 정확히 100개만 발급되어야 함
//        assertThat(finalCoupon.getIssuedQuantity()).isEqualTo(100);
//        assertThat(couponIssueRepository.count()).isEqualTo(100);
//        assertThat(successCount.get()).isEqualTo(100);
//        assertThat(failCount.get()).isEqualTo(900);
//
//        log.info("✅ 낙관적 락으로 정합성이 보장되었습니다!");
//    }
//
//    @Test
//    @DisplayName("성능 비교: Pessimistic Lock vs Optimistic Lock")
//    void testPerformanceComparison() throws InterruptedException {
//        int threadCount = 100;
//
//        // 비관적 락 테스트
//        Coupon pessimisticCoupon = Coupon.builder()
//                .name("비관적 락 테스트 쿠폰")
//                .totalQuantity(50)
//                .build();
//        couponRepository.save(pessimisticCoupon);
//
//        long pessimisticStartTime = System.currentTimeMillis();
//        ExecutorService pessimisticExecutor = Executors.newFixedThreadPool(10);
//        CountDownLatch pessimisticLatch = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            long userId = i;
//            pessimisticExecutor.submit(() -> {
//                try {
//                    couponServicePessimistic.issueCoupon(pessimisticCoupon.getId(), userId);
//                } catch (Exception e) {
//                    // ignore
//                } finally {
//                    pessimisticLatch.countDown();
//                }
//            });
//        }
//
//        pessimisticLatch.await();
//        pessimisticExecutor.shutdown();
//        long pessimisticDuration = System.currentTimeMillis() - pessimisticStartTime;
//
//        // 낙관적 락 테스트
//        Coupon optimisticCoupon = Coupon.builder()
//                .name("낙관적 락 테스트 쿠폰")
//                .totalQuantity(50)
//                .build();
//        couponRepository.save(optimisticCoupon);
//
//        long optimisticStartTime = System.currentTimeMillis();
//        ExecutorService optimisticExecutor = Executors.newFixedThreadPool(10);
//        CountDownLatch optimisticLatch = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            long userId = i + 1000; // 다른 userId 사용
//            optimisticExecutor.submit(() -> {
//                try {
//                    couponServiceOptimistic.issueCouponWithRetry(optimisticCoupon.getId(), userId);
//                } catch (Exception e) {
//                    // ignore
//                } finally {
//                    optimisticLatch.countDown();
//                }
//            });
//        }
//
//        optimisticLatch.await();
//        optimisticExecutor.shutdown();
//        long optimisticDuration = System.currentTimeMillis() - optimisticStartTime;
//
//        log.info("=== 성능 비교 ===");
//        log.info("비관적 락 소요 시간: {}ms", pessimisticDuration);
//        log.info("낙관적 락 소요 시간: {}ms", optimisticDuration);
//        log.info("================");
//    }

