package com.example.nging.user.repository.jpa;

import com.example.nging.user.repository.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContactJpaRepository extends JpaRepository<ContactEntity, Integer> {
    Optional<ContactEntity> findByUserId(Integer userId);
}
