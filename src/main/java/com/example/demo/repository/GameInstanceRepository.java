package com.example.demo.repository;

import com.example.demo.model.GameInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 游戏实例数据访问接口
 * 提供对 GameInstance 实体的增删改查操作
 */
public interface GameInstanceRepository extends JpaRepository<GameInstance, Long> {
    
    /**
     * 查找用户的游戏历史
     * 按最后更新时间降序排列
     * @param userId 用户ID
     * @return 游戏实例列表
     */
    List<GameInstance> findByUserIdOrderByLastUpdateTimeDesc(Long userId);
}
