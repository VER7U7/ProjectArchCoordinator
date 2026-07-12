package com.VER7U7.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String refreshToken;
    @ManyToOne
    private PlayerAccount ownerAccount;
    private boolean isUsed;

    @Column(updatable = false)
    private LocalDateTime creationTime = LocalDateTime.now();

    public RefreshToken() {
    }

    public RefreshToken(String refreshToken, PlayerAccount ownerAccount, boolean isUsed) {
        this.refreshToken = refreshToken;
        this.ownerAccount = ownerAccount;
        this.isUsed = isUsed;
    }

    public Long getId() {
        return id;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public PlayerAccount getOwnerAccount() {
        return ownerAccount;
    }

    public void setOwnerAccount(PlayerAccount ownerAccount) {
        this.ownerAccount = ownerAccount;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return isUsed == that.isUsed && Objects.equals(id, that.id) && Objects.equals(refreshToken, that.refreshToken) && Objects.equals(ownerAccount, that.ownerAccount) && Objects.equals(creationTime, that.creationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, refreshToken, ownerAccount, isUsed, creationTime);
    }
}
