package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 人生经历记录实体
 * 记录用户每一年或每个关键事件的详细信息，用于回顾和生成传记
 */
@Data
@Entity
@Table(name = "life_history")
public class LifeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发生事件时的年龄 */
    private int age;
    
    /** 事件描述（通常是剧情文本或JSON） */
    @Column(columnDefinition = "TEXT")
    private String eventDescription; // 剧情描述
    
    /** 事件类型：RANDOM（随机）, CHOICE（选择）, MACRO（宏观）, SUMMARY（总结/跳过） */
    private String eventType; // RANDOM, CHOICE, MACRO, SUMMARY
    
    /** 影响分析（对属性数值的改变记录） */
    @Column(columnDefinition = "TEXT")
    private String impactAnalysis; // 对数值的影响分析
}
