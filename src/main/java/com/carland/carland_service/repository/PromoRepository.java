package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Promo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoRepository extends JpaRepository<Promo, Long> {

    List<Promo> findAllByOrderByIdDesc();

    Optional<Promo> findByCodeKey(String codeKey);

    Optional<Promo> findByBannerId(Long bannerId);
}
