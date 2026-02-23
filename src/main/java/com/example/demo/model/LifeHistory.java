package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "life_history")
public class LifeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int age;
    
    @Column(columnDefinition = "TEXT")
    private String eventDescription; // 剧情描述
    
    private String eventType; // RANDOM, CHOICE, MACRO, SUMMARY
    
    @Column(columnDefinition = "TEXT")
    private String impactAnalysis; // 对数值的影响分析
}
