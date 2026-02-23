package com.example.demo.repository;

import com.example.demo.model.GameInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameInstanceRepository extends JpaRepository<GameInstance, Long> {
    List<GameInstance> findByUserIdOrderByLastUpdateTimeDesc(Long userId);
}
