package com.yuuki.demo.facade;

import com.yuuki.demo.dto.CouponIssueResponse;
import com.yuuki.demo.repository.LockRepository;
import com.yuuki.demo.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponNamedLockFacade {

    private final LockRepository lockRepository;
    private final CouponService couponService;

    @Transactional
    public void issueCoupon(Long couponId, Long userId) {
        try{
            lockRepository.getLock(couponId.toString());
            couponService.issueCoupon(couponId, userId);
        } finally {
            lockRepository.releaseLock(couponId.toString());
        }

    }
}
