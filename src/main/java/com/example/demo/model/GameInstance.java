package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 游戏实例实体
 * 代表一次完整的游戏进程，关联用户、模板和当前档案状态
 */
@Entity
public class GameInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的用户ID */
    private Long userId;
    
    /** 使用的模板ID（可选） */
    private Long templateId;
    
    /** 关联的当前用户档案（存储游戏状态） */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private UserProfile userProfile;

    /** 游戏状态：ACTIVE（进行中）, FINISHED（已结束） */
    private String status; // ACTIVE, FINISHED
    
    /** 最后更新时间 */
    private LocalDateTime lastUpdateTime;

    public GameInstance() {
        this.lastUpdateTime = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}
