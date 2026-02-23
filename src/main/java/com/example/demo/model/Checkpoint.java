package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "checkpoints")
public class Checkpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String profileId; // Which user this backup belongs to
    private int age; // The age at this checkpoint
    
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(columnDefinition = "LONGTEXT")
    private String profileJson; // Full snapshot of the UserProfile
}
