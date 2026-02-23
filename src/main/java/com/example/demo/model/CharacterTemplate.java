package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CharacterTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String basicInfo; // JSON string

    @Column(columnDefinition = "TEXT")
    private String familyBackground; // JSON string
    
    @Column(columnDefinition = "TEXT")
    private String initialAttributes; // JSON string

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
