package com.practice.knowheart.repository;

import com.practice.knowheart.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByAuthToken(String authToken);
}
