package com.yuuki.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponIssueResponse {
    private boolean success;
    private String message;
    private Long couponIssueId;
    private Integer remainingQuantity;


    public static CouponIssueResponse success(Long couponIssueId, Integer remainingQuantity) {
        return new CouponIssueResponse(true, "쿠폰 발급 성공", couponIssueId, remainingQuantity);
    }

    public static CouponIssueResponse fail(String message) {
        return new CouponIssueResponse(false, message, null, null);
    }
}
