package com.example.demo.repository;

import com.example.demo.model.CharacterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CharacterTemplateRepository extends JpaRepository<CharacterTemplate, Long> {
    List<CharacterTemplate> findByUserId(Long userId);
}
