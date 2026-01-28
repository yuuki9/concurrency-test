package com.yuuki.demo.repository;

import com.yuuki.demo.domain.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 비관적 락 - 배타적 락 (Pessimistic Write Lock)
     * 다른 트랜잭션에서 읽기/쓰기 불가
     * Lock 어노테이션을 이용해서 구현
     */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * 별도 어노테이션 없이도 @Version 필드가 있으면 자동으로 낙관적 락 적용
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdWithOptimisticLock(@Param("id") Long id);
}
