package com.yuuki.demo.controller;

import com.yuuki.demo.domain.Coupon;
import com.yuuki.demo.dto.CouponIssueRequest;
import com.yuuki.demo.dto.CouponIssueResponse;
import com.yuuki.demo.dto.CouponResponse;
import com.yuuki.demo.repository.CouponRepository;
import com.yuuki.demo.service.CouponServiceSync;
import com.yuuki.demo.service.CouponServicePessimistic;
import com.yuuki.demo.service.CouponServiceOptimistic;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository;
    private final CouponServiceSync couponServiceSync;
    private final CouponServicePessimistic couponServicePessimistic;
    private final CouponServiceOptimistic couponServiceOptimistic;

    /**
     * 쿠폰 생성
     */
    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @RequestParam String name,
            @RequestParam Integer quantity) {
        Coupon coupon = Coupon.builder()
                .name(name)
                .totalQuantity(quantity)
                .build();
        couponRepository.save(coupon);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    /**
     * 쿠폰 조회
     */
    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    /**
     * Synchronized를 사용한 쿠폰 발급 (실패 케이스)
     */
    @PostMapping("/sync/issue")
    public ResponseEntity<CouponIssueResponse> issueCouponSync(@RequestBody CouponIssueRequest request) {
        CouponIssueResponse response = couponServiceSync.issueCoupon(
                request.getCouponId(), 
                request.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 비관적 락을 사용한 쿠폰 발급
     */
    @PostMapping("/pessimistic/issue")
    public ResponseEntity<CouponIssueResponse> issueCouponPessimistic(@RequestBody CouponIssueRequest request) {
        CouponIssueResponse response = couponServicePessimistic.issueCoupon(
                request.getCouponId(), 
                request.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 낙관적 락을 사용한 쿠폰 발급 (재시도 포함)
     */
    @PostMapping("/optimistic/issue")
    public ResponseEntity<CouponIssueResponse> issueCouponOptimistic(@RequestBody CouponIssueRequest request) {
        CouponIssueResponse response = couponServiceOptimistic.issueCouponWithRetry(
                request.getCouponId(), 
                request.getUserId()
        );
        return ResponseEntity.ok(response);
    }
}
