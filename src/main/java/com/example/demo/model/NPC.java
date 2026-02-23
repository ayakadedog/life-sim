package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * NPC 实体
 * 代表游戏中的非玩家角色，如父母、配偶、朋友等
 */
@Data
@Entity
@Table(name = "npcs")
public class NPC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    private String name;
    
    /** 关系类型：FATHER, MOTHER, PARTNER, FRIEND, CHILD 等 */
    private String relation; // FATHER, MOTHER, PARTNER, FRIEND
    
    /** 当前年龄 */
    private int age;
    
    /** 状态：HEALTHY（健康）, SICK（生病）, DEAD（去世）等 */
    private String status; // HEALTHY, SICK, RICH, POOR, DEAD
    
    /** 亲密度 (0-100) */
    private int intimacy; // 亲密度 0-100
    
    /** 当前状况描述（由 AI 生成） */
    @Column(columnDefinition = "TEXT")
    private String currentSituation; // 当前状况描述
}
