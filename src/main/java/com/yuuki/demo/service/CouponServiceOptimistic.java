package com.yuuki.demo.service;

import com.yuuki.demo.domain.Coupon;
import com.yuuki.demo.domain.CouponIssue;
import com.yuuki.demo.dto.CouponIssueResponse;
import com.yuuki.demo.repository.CouponIssueRepository;
import com.yuuki.demo.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 낙관적 락(Optimistic Lock)을 사용한 동시성 제어
 * 
 * 특징:
 * - @Version 필드를 사용하여 버전 체크
 * - 데이터 수정 시점에 버전을 확인하여 충돌 감지
 * - 충돌 발생 시 재시도 로직 필요
 * 
 * 장점:
 * - 락을 걸지 않아 성능이 좋음
 * - 동시 처리량이 높음
 * - 충돌이 적은 환경에 적합
 * 
 * 단점:
 * - 충돌 시 재시도 로직 필요
 * - 충돌이 빈번하면 비효율적
 * - 재시도 횟수가 많아질 수 있음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceOptimistic {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    
    private static final int MAX_RETRIES = 50;
    private static final long RETRY_DELAY_MS = 50;

    /**
     * 낙관적 락을 사용한 쿠폰 발급 (기본)
     * 
     * @Version 필드를 사용하여 업데이트 시 버전 체크
     * 충돌 발생 시 ObjectOptimisticLockingFailureException 발생
     * 재시도 로직은 호출하는 쪽에서 구현
     */
    @Transactional
    public CouponIssueResponse issueCoupon(Long couponId, Long userId) {
        log.info("[OptimisticLock] 쿠폰 발급 시도 - couponId: {}, userId: {}", couponId, userId);

        try {
            // 1. 낙관적 락으로 쿠폰 조회
            Coupon coupon = couponRepository.findByIdWithOptimisticLock(couponId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

            // 2. 이미 발급받은 사용자인지 확인
            if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
                log.warn("[OptimisticLock] 이미 발급받은 사용자 - couponId: {}, userId: {}", couponId, userId);
                return CouponIssueResponse.fail("이미 발급받은 쿠폰입니다.");
            }

            // 3. 쿠폰 발급 가능 여부 확인 및 발급
            if (!coupon.canIssue()) {
                log.warn("[OptimisticLock] 쿠폰 재고 부족 - couponId: {}, userId: {}", couponId, userId);
                return CouponIssueResponse.fail("쿠폰이 모두 발급되었습니다.");
            }

            coupon.issue();

            // 4. 쿠폰 발급 이력 저장
            CouponIssue couponIssue = CouponIssue.builder()
                    .couponId(couponId)
                    .userId(userId)
                    .build();
            couponIssueRepository.save(couponIssue);

            log.info("[OptimisticLock] 쿠폰 발급 성공 - couponId: {}, userId: {}, issueId: {}", 
                    couponId, userId, couponIssue.getId());

            return CouponIssueResponse.success(couponIssue.getId(), coupon.getRemainingQuantity());

        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("[OptimisticLock] 낙관적 락 충돌 - couponId: {}, userId: {}", couponId, userId);
            throw e; // 재시도는 호출하는 쪽에서 처리
        }
    }

    /**
     * 낙관적 락 재시도 로직이 포함된 쿠폰 발급
     * 
     * 최대 재시도 횟수만큼 시도하며, 실패 시 예외 발생
     */
    public CouponIssueResponse issueCouponWithRetry(Long couponId, Long userId) {
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                return issueCoupon(couponId, userId);
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("[OptimisticLock] 재시도 {}/{} - couponId: {}, userId: {}", 
                        retryCount, MAX_RETRIES, couponId, userId);
                
                if (retryCount >= MAX_RETRIES) {
                    log.error("[OptimisticLock] 최대 재시도 횟수 초과 - couponId: {}, userId: {}", couponId, userId);
                    return CouponIssueResponse.fail("쿠폰 발급에 실패했습니다. 다시 시도해주세요.");
                }

                // 짧은 대기 후 재시도
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return CouponIssueResponse.fail("쿠폰 발급 중 오류가 발생했습니다.");
                }
            }
        }

        return CouponIssueResponse.fail("쿠폰 발급에 실패했습니다.");
    }

    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }
}
