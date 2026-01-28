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


    //Pessimistic Lock 실제로 데이터에 Lock를 걸어서 정합성을 맞추는 역할
    @Test
    @DisplayName("쿠폰 동시에 100개 비관적락 적용")
    void 쿠폰발급_동시에_100개_비관적락() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.execute(() -> {
                try {
                    CouponIssueResponse response = couponServicePessimistic.issueCoupon(testCoupon.getId(), userId);
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
    
    //Optimistic Lock Lock을 이용하지 않고 버전을 이용하는 락관적락
    //별도의 락을 잡지 않으므로 비관적락보다 성능상 이점 재시도 비용이 커 질 경우 비용이 더 커질수 있음
    @Test
    @DisplayName("쿠폰 동시에 100개 낙관적락 적용")
    void 쿠폰발급_동시에_100개_낙관적락() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.execute(() -> {
                try {
                    CouponIssueResponse response = couponServiceOptimistic.issueCouponWithRetry(testCoupon.getId(), userId);
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


