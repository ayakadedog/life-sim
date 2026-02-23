package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存档点实体
 * 用于在关键节点保存用户档案的快照，支持回溯功能
 */
@Data
@Entity
@Table(name = "checkpoints")
public class Checkpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的档案ID */
    private String profileId; // Which user this backup belongs to
    
    /** 存档时的年龄 */
    private int age; // The age at this checkpoint
    
    /** 存档时间 */
    private LocalDateTime timestamp = LocalDateTime.now();

    /** 完整的档案JSON快照 */
    @Column(columnDefinition = "LONGTEXT")
    private String profileJson; // Full snapshot of the UserProfile
}
