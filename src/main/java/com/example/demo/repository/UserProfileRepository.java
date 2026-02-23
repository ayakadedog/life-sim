package com.example.demo.repository;

import com.example.demo.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户档案数据访问接口
 * 提供对 UserProfile 实体的增删改查操作
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
}
