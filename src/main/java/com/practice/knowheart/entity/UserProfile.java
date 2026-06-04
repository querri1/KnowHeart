package com.practice.knowheart.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "relationship", length = 20)
    private String relationship;

    @Column(name = "interests", length = 2000)
    private String interests;

    @Column(name = "preferences", length = 2000)
    private String preferences;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 无参构造
    public UserProfile() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // 生成画像摘要（用于注入到 System Prompt）
    public String toProfileSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户画像\n");
        if (name != null && !name.isEmpty()) sb.append("- 称呼：").append(name).append("\n");
        if (age != null) sb.append("- 年龄：").append(age).append("岁\n");
        if (gender != null && !gender.isEmpty()) sb.append("- 性别：").append(gender).append("\n");
        if (city != null && !city.isEmpty()) sb.append("- 城市：").append(city).append("\n");
        if (relationship != null && !relationship.isEmpty()) sb.append("- 恋爱状态：").append(relationship).append("\n");
        return sb.toString();
    }
}