package com.practice.knowheart.dto;

import com.practice.knowheart.entity.UserProfile;

public class UserCenterResponse {

    private String userId;
    private String username;
    private String nickname;
    private UserProfile profile;
    private java.time.LocalDateTime registeredAt;

    public UserCenterResponse() {}

    public UserCenterResponse(String userId, String username, String nickname,
                              UserProfile profile, java.time.LocalDateTime registeredAt) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.profile = profile;
        this.registeredAt = registeredAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public java.time.LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(java.time.LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
