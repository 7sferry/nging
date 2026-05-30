package com.example.nging.user.repository.jpa;

import com.example.nging.user.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {
}
