package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.TableEntity;
import org.example.pingpongsystem.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokenRepository extends JpaRepository<TokenEntity, Long> {

    public TokenEntity findByToken(String token);

    void deleteByToken(String token);
}
