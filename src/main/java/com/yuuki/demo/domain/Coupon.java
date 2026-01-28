package com.yuuki.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity;

    @Version  // 낙관적 락을 위한 버전
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Coupon(String name, Integer totalQuantity) {
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 발급 가능 여부 확인
     */
    public boolean canIssue() {
        return issuedQuantity < totalQuantity;
    }

    /**
     * 쿠폰 발급
     */
    public void issue() {
        if (!canIssue()) {
            throw new IllegalStateException("쿠폰이 모두 발급 되었습니다");
        }
        this.issuedQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 남은 쿠폰 수량
     */
    public int getRemainingQuantity() {
        return totalQuantity - issuedQuantity;
    }
}
