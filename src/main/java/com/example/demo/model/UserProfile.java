package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private String id;

    // --- 0. Game Settings ---
    private String difficulty = "Normal"; // Easy, Normal, Hard, Hell

    // --- 1. 基础硬指标 (Hard Stats) ---
    @Embedded
    private BasicInfo basicInfo = new BasicInfo();
    @Embedded
    private EconomicStatus economicStatus = new EconomicStatus();

    // --- 2. 软性资源 (Soft Assets) ---
    @Embedded
    private FamilyBackground familyBackground = new FamilyBackground();
    @Embedded
    private HealthStatus healthStatus = new HealthStatus();
    @Embedded
    private SocialConnections socialConnections = new SocialConnections();

    // --- 3. 心理内核 (Psychological Core) ---
    @ElementCollection
    @CollectionTable(name = "user_personality", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "trait_name")
    @Column(name = "trait_value")
    private Map<String, Integer> personalityTraits = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "user_core_values", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "value_item")
    private List<String> coreValues = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_inner_conflicts", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "conflict_item")
    private List<String> innerConflicts = new ArrayList<>();

    // --- 4. 动态状态 (Simulation State) ---
    private int currentAge;

    @Column(columnDefinition = "TEXT")
    private String currentScenario;
    
    // Dynamic choices for the next turn
    @ElementCollection
    @CollectionTable(name = "user_available_choices", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "choice_text")
    private List<String> availableChoices = new ArrayList<>();

    // --- 5. 传承系统 (Legacy) ---
    private int generation = 1; // 第几代
    private String parentProfileId; // 父辈ID

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private List<LifeHistory> lifeHistory = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private List<NPC> npcs = new ArrayList<>();

    @Data
    @Embeddable
    public static class BasicInfo {
        private String name;
        private int startAge; // 初始年龄
        private String gender;
        private String location;
        private String educationLevel; // Include Overseas Student
        private String profession;
        @Column(columnDefinition = "TEXT")
        private String lifeExperiences; // Free text for past experiences
    }

    @Data
    @Embeddable
    public static class EconomicStatus {
        private double savings;
        private double debt;
        private double monthlyIncome;
        
        @ElementCollection
        @CollectionTable(name = "user_assets", joinColumns = @JoinColumn(name = "profile_id"))
        @Column(name = "asset_name")
        private List<String> assets = new ArrayList<>();
    }

    @Data
    @Embeddable
    public static class FamilyBackground {
        private String parentsStatus;
        private String economicSupport; // e.g. "Full Support", "Independent"
        private int siblingCount;
        private String familyAssets; // e.g. "10M", "2 Houses"
        private String fatherProfession;
        private String motherProfession;
    }

    @Data
    @Embeddable
    public static class HealthStatus {
        private int energyLevel; // 0-100
        
        @ElementCollection
        @CollectionTable(name = "user_chronic_conditions", joinColumns = @JoinColumn(name = "profile_id"))
        @Column(name = "condition_name")
        private List<String> chronicConditions = new ArrayList<>();
        
        private String familyHistory;
    }

    @Data
    @Embeddable
    public static class SocialConnections {
        private String relationshipStatus;
        private String socialCircleQuality;
    }
}
