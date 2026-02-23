package com.example.demo.repository;

import com.example.demo.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    List<Checkpoint> findByProfileIdOrderByAgeDesc(String profileId);
}
