package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 角色模板实体
 * 用于存储用户创建的可复用角色配置，包括基本信息、家庭背景和初始属性
 */
@Entity
public class CharacterTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的用户ID */
    private Long userId;
    
    /** 模板名称（通常是角色名） */
    private String name;

    /** 基本信息（JSON字符串） */
    @Column(columnDefinition = "TEXT")
    private String basicInfo; // JSON string

    /** 家庭背景（JSON字符串） */
    @Column(columnDefinition = "TEXT")
    private String familyBackground; // JSON string
    
    /** 初始属性（JSON字符串，如经济状况等） */
    @Column(columnDefinition = "TEXT")
    private String initialAttributes; // JSON string

    /** 创建时间 */
    private LocalDateTime createTime;

    public CharacterTemplate() {
        this.createTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBasicInfo() { return basicInfo; }
    public void setBasicInfo(String basicInfo) { this.basicInfo = basicInfo; }
    public String getFamilyBackground() { return familyBackground; }
    public void setFamilyBackground(String familyBackground) { this.familyBackground = familyBackground; }
    public String getInitialAttributes() { return initialAttributes; }
    public void setInitialAttributes(String initialAttributes) { this.initialAttributes = initialAttributes; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
