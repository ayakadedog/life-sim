package com.example.demo.repository;

import com.example.demo.model.CharacterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 角色模板数据访问接口
 * 提供对 CharacterTemplate 实体的增删改查操作
 */
public interface CharacterTemplateRepository extends JpaRepository<CharacterTemplate, Long> {
    
    /**
     * 根据用户ID查找所有角色模板
     * @param userId 用户ID
     * @return 该用户创建的角色模板列表
     */
    List<CharacterTemplate> findByUserId(Long userId);
}
