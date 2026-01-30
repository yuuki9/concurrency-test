package com.yuuki.demo.service;

import com.yuuki.demo.domain.Coupon;
import com.yuuki.demo.domain.CouponIssue;
import com.yuuki.demo.dto.CouponIssueResponse;
import com.yuuki.demo.repository.CouponIssueRepository;
import com.yuuki.demo.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CouponIssueResponse issueCoupon(Long couponId, Long userId) {

        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 2. 이미 발급받은 사용자인지 확인
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            return CouponIssueResponse.fail("이미 발급받은 쿠폰입니다.");
        }

        // 3. 쿠폰 발급 가능 여부 확인 및 발급
        if (!coupon.canIssue()) {
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


        return CouponIssueResponse.success(couponIssue.getId(), coupon.getRemainingQuantity());
    }

}
