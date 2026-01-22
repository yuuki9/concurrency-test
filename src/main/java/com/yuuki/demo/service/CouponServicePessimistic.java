package com.yuuki.demo.service;

import com.yuuki.demo.domain.Coupon;
import com.yuuki.demo.domain.CouponIssue;
import com.yuuki.demo.dto.CouponIssueResponse;
import com.yuuki.demo.repository.CouponIssueRepository;
import com.yuuki.demo.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비관적 락(Pessimistic Lock)을 사용한 동시성 제어
 * 
 * 특징:
 * - SELECT ... FOR UPDATE 사용
 * - 데이터를 읽는 시점에 락을 걸어 다른 트랜잭션의 접근을 차단
 * - 트랜잭션이 끝날 때까지 다른 트랜잭션은 대기
 * 
 * 장점:
 * - 충돌이 빈번한 경우 효율적
 * - 확실한 동시성 제어
 * - 데이터 정합성 보장
 * 
 * 단점:
 * - 데드락 가능성
 * - 성능 저하 (대기 시간 증가)
 * - 동시 처리량 감소
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServicePessimistic {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    /**
     * 비관적 락을 사용한 쿠폰 발급
     * 
     * SELECT ... FOR UPDATE를 사용하여 row-level lock
     * 트랜잭션이 끝날 때까지 다른 트랜잭션은 대기
     */
    @Transactional
    public CouponIssueResponse issueCoupon(Long couponId, Long userId) {
        log.info("[PessimisticLock] 쿠폰 발급 시작 - couponId: {}, userId: {}", couponId, userId);

        // 1. 비관적 락으로 쿠폰 조회
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 이미 발급받은 사용자인지 확인
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            log.warn("[PessimisticLock] 이미 발급받은 사용자 - couponId: {}, userId: {}", couponId, userId);
            return CouponIssueResponse.fail("이미 발급받은 쿠폰입니다.");
        }

        // 3. 쿠폰 발급 가능 여부 확인 및 발급
        if (!coupon.canIssue()) {
            log.warn("[PessimisticLock] 쿠폰 재고 부족 - couponId: {}, userId: {}", couponId, userId);
            return CouponIssueResponse.fail("쿠폰이 모두 발급되었습니다.");
        }

        coupon.issue();

        // 4. 쿠폰 발급 이력 저장
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();
        couponIssueRepository.save(couponIssue);

        log.info("[PessimisticLock] 쿠폰 발급 성공 - couponId: {}, userId: {}, issueId: {}", 
                couponId, userId, couponIssue.getId());

        return CouponIssueResponse.success(couponIssue.getId(), coupon.getRemainingQuantity());
    }

    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }
}
