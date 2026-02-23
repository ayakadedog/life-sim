package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 提供对 User 实体的增删改查操作
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据手机号查找用户
     * @param phone 手机号
     * @return 包含用户的 Optional 对象，如果未找到则为空
     */
    Optional<User> findByPhone(String phone);
}
