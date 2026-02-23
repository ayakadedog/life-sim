package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "npcs")
public class NPC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String relation; // FATHER, MOTHER, PARTNER, FRIEND
    private int age;
    private String status; // HEALTHY, SICK, RICH, POOR, DEAD
    private int intimacy; // 亲密度 0-100
    
    @Column(columnDefinition = "TEXT")
    private String currentSituation; // 当前状况描述
}
