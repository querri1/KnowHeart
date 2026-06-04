package com.practice.knowheart.service;

import com.practice.knowheart.entity.UserProfile;
import com.practice.knowheart.repository.UserProfileRepository;
import com.practice.knowheart.service.ProfileExtractorService.UserProfileDto;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final ProfileExtractorService extractorService;

    public UserProfileService(UserProfileRepository profileRepository,
                              ProfileExtractorService extractorService) {
        this.profileRepository = profileRepository;
        this.extractorService = extractorService;
    }

    /**
     * 获取用户画像（如果存在）
     */
    public Optional<UserProfile> getProfile(String userId) {
        return profileRepository.findById(userId);
    }

    /**
     * 从消息中提取并更新用户画像
     */
    public UserProfile updateFromMessage(String userId, String message) {
        // 提取新信息
        UserProfileDto extracted = extractorService.extract(message, userId);

        if (!extracted.hasAnyInfo()) {
            // 没有新信息，返回现有画像
            return profileRepository.findById(userId).orElse(null);
        }

        // 获取或创建画像
        UserProfile profile = profileRepository.findById(userId)
                .orElse(new UserProfile());

        boolean updated = false;

        if (extracted.name != null) {
            profile.setName(extracted.name);
            updated = true;
        }
        if (extracted.age != null) {
            profile.setAge(extracted.age);
            updated = true;
        }
        if (extracted.city != null) {
            profile.setCity(extracted.city);
            updated = true;
        }
        if (extracted.relationship != null) {
            profile.setRelationship(extracted.relationship);
            updated = true;
        }

        if (updated) {
            profile.setUserId(userId);
            if (profile.getCreatedAt() == null) {
                profile.setCreatedAt(LocalDateTime.now());
            }
            profile.setUpdatedAt(LocalDateTime.now());
            return profileRepository.save(profile);
        }

        return profile;
    }

    /**
     * 获取用户画像摘要（用于注入 System Prompt）
     */
    public String getProfileSummary(String userId) {
        Optional<UserProfile> profile = getProfile(userId);
        if (profile.isPresent() && hasValidInfo(profile.get())) {
            return profile.get().toProfileSummary();
        }
        return "";
    }

    private boolean hasValidInfo(UserProfile profile) {
        return profile.getName() != null || profile.getAge() != null ||
                profile.getCity() != null || profile.getRelationship() != null;
    }
}