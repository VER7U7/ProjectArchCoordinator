package com.VER7U7.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class PlayerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String playerNickName;
    @JsonIgnore
    private String password;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime creationTime = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PlayerAccount() {
    }

    public PlayerAccount(String playerNickName, String password) {
        this.playerNickName = playerNickName;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getPlayerNickName() {
        return playerNickName;
    }

    public void setPlayerNickName(String playerNickName) {
        this.playerNickName = playerNickName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerAccount that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(playerNickName, that.playerNickName) && Objects.equals(password, that.password) && Objects.equals(creationTime, that.creationTime) && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, playerNickName, password, creationTime, updatedAt);
    }
}
