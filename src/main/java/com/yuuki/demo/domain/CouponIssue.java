package com.yuuki.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_issue",
        indexes = @Index(name = "idx_coupon_user", columnList = "coupon_id,user_id", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Builder
    public CouponIssue(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
        this.issuedAt = LocalDateTime.now();
    }
}
