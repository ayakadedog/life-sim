package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户档案实体
 * 核心数据模型，存储用户的游戏状态、属性、资产、关系网及历史记录
 */
@Data
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private String id;

    // --- 0. Game Settings ---
    /** 游戏难度：Easy, Normal, Hard, Hell */
    private String difficulty = "Normal"; // Easy, Normal, Hard, Hell

    // --- 1. 基础硬指标 (Hard Stats) ---
    /** 基础个人信息 */
    @Embedded
    private BasicInfo basicInfo = new BasicInfo();
    
    /** 经济状况 */
    @Embedded
    private EconomicStatus economicStatus = new EconomicStatus();

    // --- 2. 软性资源 (Soft Assets) ---
    /** 家庭背景 */
    @Embedded
    private FamilyBackground familyBackground = new FamilyBackground();
    
    /** 健康状况 */
    @Embedded
    private HealthStatus healthStatus = new HealthStatus();
    
    /** 社交关系概况 */
    @Embedded
    private SocialConnections socialConnections = new SocialConnections();

    // --- 3. 心理内核 (Psychological Core) ---
    /** 
     * 人格特质 (Big Five or custom traits)
     * Key: 特质名称 (e.g., "Openness", "Resilience")
     * Value: 数值 (0-100)
     */
    @ElementCollection
    @CollectionTable(name = "user_personality", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "trait_name")
    @Column(name = "trait_value")
    private Map<String, Integer> personalityTraits = new HashMap<>();

    /** 核心价值观列表 */
    @ElementCollection
    @CollectionTable(name = "user_core_values", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "value_item")
    private List<String> coreValues = new ArrayList<>();

    /** 内心冲突列表 */
    @ElementCollection
    @CollectionTable(name = "user_inner_conflicts", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "conflict_item")
    private List<String> innerConflicts = new ArrayList<>();

    // --- 4. 动态状态 (Simulation State) ---
    /** 当前年龄 */
    private int currentAge;

    /** 当前场景描述（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String currentScenario;
    
    /** 长期记忆（滚动更新的文本摘要） */
    @Column(columnDefinition = "TEXT")
    private String longTermMemory = ""; 
    
    /** 下一回合的可用选择 */
    @ElementCollection
    @CollectionTable(name = "user_available_choices", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "choice_text")
    private List<String> availableChoices = new ArrayList<>();

    // --- 5. 传承系统 (Legacy) ---
    /** 第几代 */
    private int generation = 1; // 第几代
    
    /** 父辈档案ID */
    private String parentProfileId; // 父辈ID

    /** 人生履历列表 */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private List<LifeHistory> lifeHistory = new ArrayList<>();

    /** NPC 列表 */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private List<NPC> npcs = new ArrayList<>();

    /** 基础信息嵌入类 */
    @Data
    @Embeddable
    public static class BasicInfo {
        /** 姓名 */
        private String name;
        /** 初始年龄 */
        private int startAge; // 初始年龄
        /** 性别 */
        private String gender;
        /** 所在地 */
        private String location;
        /** 教育程度 */
        private String educationLevel; // Include Overseas Student
        /** 职业 */
        private String profession;
        /** 既往经历描述 */
        @Column(columnDefinition = "TEXT")
        private String lifeExperiences; // Free text for past experiences
    }

    /** 经济状况嵌入类 */
    @Data
    @Embeddable
    public static class EconomicStatus {
        /** 存款 */
        private double savings;
        /** 负债 */
        private double debt;
        /** 月收入 */
        private double monthlyIncome;
        
        /** 资产列表 */
        @ElementCollection
        @CollectionTable(name = "user_assets", joinColumns = @JoinColumn(name = "profile_id"))
        @Column(name = "asset_name")
        private List<String> assets = new ArrayList<>();
    }

    /** 家庭背景嵌入类 */
    @Data
    @Embeddable
    public static class FamilyBackground {
        /** 父母状况 */
        private String parentsStatus;
        /** 经济支持力度 */
        private String economicSupport; // e.g. "Full Support", "Independent"
        /** 兄弟姐妹数量 */
        private int siblingCount;
        /** 家庭资产概况 */
        private String familyAssets; // e.g. "10M", "2 Houses"
        /** 父亲职业 */
        private String fatherProfession;
        /** 母亲职业 */
        private String motherProfession;
    }

    /** 健康状况嵌入类 */
    @Data
    @Embeddable
    public static class HealthStatus {
        /** 精力值 (0-100) */
        private int energyLevel; // 0-100
        
        /** 慢性病/状态列表 */
        @ElementCollection
        @CollectionTable(name = "user_chronic_conditions", joinColumns = @JoinColumn(name = "profile_id"))
        @Column(name = "condition_name")
        private List<String> chronicConditions = new ArrayList<>();
        
        /** 家族病史 */
        private String familyHistory;
    }

    /** 社交关系嵌入类 */
    @Data
    @Embeddable
    public static class SocialConnections {
        /** 情感状态 */
        private String relationshipStatus;
        /** 社交圈质量 */
        private String socialCircleQuality;
    }
}
