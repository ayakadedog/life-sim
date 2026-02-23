package com.example.demo.service;

import com.example.demo.model.Checkpoint;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.CheckpointRepository;
import com.example.demo.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 持久化服务
 * 负责管理游戏存档、检查点创建和回滚
 */
@Service
public class PersistenceService {

    @Autowired
    private CheckpointRepository checkpointRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建检查点
     * 将当前用户档案序列化为 JSON 并保存到数据库
     * @param profile 用户档案信息
     */
    public void createCheckpoint(UserProfile profile) {
        try {
            Checkpoint cp = new Checkpoint();
            cp.setProfileId(profile.getId());
            cp.setAge(profile.getCurrentAge());
            // 序列化整个对象图为 JSON
            String json = objectMapper.writeValueAsString(profile);
            cp.setProfileJson(json);
            checkpointRepository.save(cp);
        } catch (Exception e) {
            e.printStackTrace(); // 在生产环境中应使用日志记录
        }
    }

    /**
     * 回滚到指定年龄的检查点
     * @param profileId 档案ID
     * @param targetAge 目标年龄
     * @return 恢复后的用户档案
     * @throws RuntimeException 如果找不到检查点或反序列化失败
     */
    public UserProfile rollback(String profileId, int targetAge) {
        List<Checkpoint> checkpoints = checkpointRepository.findByProfileIdOrderByAgeDesc(profileId);
        for (Checkpoint cp : checkpoints) {
            if (cp.getAge() == targetAge) {
                try {
                    UserProfile restored = objectMapper.readValue(cp.getProfileJson(), UserProfile.class);
                    // 覆盖数据库中的当前状态
                    return userProfileRepository.save(restored);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to restore checkpoint", e);
                }
            }
        }
        throw new RuntimeException("Checkpoint not found for age " + targetAge);
    }
    
    /**
     * 获取历史检查点列表
     * @param profileId 档案ID
     * @return 按年龄降序排列的检查点列表
     */
    public List<Checkpoint> getHistory(String profileId) {
        return checkpointRepository.findByProfileIdOrderByAgeDesc(profileId);
    }
}
