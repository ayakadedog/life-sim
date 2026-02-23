package com.example.demo.repository;

import com.example.demo.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 存档点数据访问接口
 * 提供对 Checkpoint 实体的增删改查操作
 */
public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {
    
    /**
     * 查找指定档案的所有存档点，并按年龄降序排列
     * @param profileId 档案ID
     * @return 存档点列表
     */
    List<Checkpoint> findByProfileIdOrderByAgeDesc(String profileId);
}
