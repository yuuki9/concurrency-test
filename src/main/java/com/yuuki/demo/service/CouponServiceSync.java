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
 * Synchronized를 사용한 동시성 제어
 * 
 * 문제점:
 * - 단일 서버 환경에서만 작동
 * - 멀티 스레드 환경에서는 동작하나, 멀티 인스턴스(분산 환경)에서는 동작하지 않음
 * - 트랜잭션이 메서드 종료 후 커밋되므로, synchronized 블록을 벗어난 후 실제 DB 반영까지 시간차 발생
 * 
 * 이 버전은 동시성 제어 실패 케이스를 확인하기 위한 용도
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceSync {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    /**
     * synchronized 키워드를 사용한 동시성 제어 (실패 케이스)
     * 
     * 문제점:
     * 1. JVM 레벨의 락이므로 단일 인스턴스에서만 작동
     * 2. @Transactional과 함께 사용 시, 트랜잭션 커밋 전에 락이 해제됨
     */
    //@Transactional
    public synchronized CouponIssueResponse issueCoupon(Long couponId, Long userId) {

        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 이미 발급받은 사용자인지 확인
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            return CouponIssueResponse.fail("이미 발급받은 쿠폰입니다.");
        }

        // 3. 쿠폰 발급 가능 여부 확인 및 발급
        if (!coupon.canIssue()) {
            log.warn("쿠폰 재고 부족 - couponId: {}, userId: {}", couponId, userId);
            return CouponIssueResponse.fail("쿠폰이 모두 발급되었습니다.");
        }

        coupon.issue();
        couponRepository.save(coupon);

        // 4. 쿠폰 발급 이력 저장
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();
        couponIssueRepository.save(couponIssue);

        log.info("[Synchronized] 쿠폰 발급 성공 - couponId: {}, userId: {}, issueId: {}", 
                couponId, userId, couponIssue.getId());

        return CouponIssueResponse.success(couponIssue.getId(), coupon.getRemainingQuantity());
    }

    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }
}
