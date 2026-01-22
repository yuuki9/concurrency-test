package com.yuuki.demo.dto;

import com.yuuki.demo.domain.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String name;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer remainingQuantity;

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.getRemainingQuantity()
        );
    }
}
