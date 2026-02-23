package com.example.demo.service;

import com.example.demo.model.NPC;
import com.example.demo.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * NPC 服务
 * 负责管理游戏中的非玩家角色（NPC）的演化和状态更新
 */
@Service
public class NPCService {

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private PromptService promptService;

    /**
     * 演化所有 NPC
     * 遍历用户的 NPC 列表，增加年龄，调用 AI 生成新的状态，并更新健康状况
     * @param profile 用户档案信息
     */
    public void evolveNPCs(UserProfile profile) {
        List<NPC> npcs = profile.getNpcs();
        if (npcs == null || npcs.isEmpty()) return;

        for (NPC npc : npcs) {
            // 年龄增长
            npc.setAge(npc.getAge() + 1);
            
            // 构建 NPC 演化提示词并调用 AI
            String prompt = promptService.buildNPCEvolutionPrompt(npc, profile);
            String update = deepSeekService.call("You are a NPC engine.", prompt);
            
            // 更新 NPC 当前状况
            npc.setCurrentSituation(update);
            
            // 简单的状态判定逻辑
            if (update.contains("病") || update.contains("住院")) {
                npc.setStatus("SICK");
            } else if (update.contains("去世") || update.contains("死")) {
                npc.setStatus("DEAD");
            }
        }
    }
}
