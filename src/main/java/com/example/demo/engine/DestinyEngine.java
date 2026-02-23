package com.example.demo.engine;

import com.example.demo.model.UserProfile;
import com.example.demo.service.DeepSeekService;
import com.example.demo.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Random;

/**
 * 命运引擎
 * 负责游戏中的随机事件判定和结果计算
 */
@Component
public class DestinyEngine {

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private PromptService promptService;

    private final Random random = new Random();

    /**
     * 判定行动结果
     * 根据用户档案、行动、宏观事件和随机因子，调用 AI 判定行动的成败及后果
     * @param profile 用户档案
     * @param action 用户采取的行动
     * @param macroEvent 当前宏观事件
     * @return 结果分析文本
     */
    public String determineOutcome(UserProfile profile, String action, String macroEvent) {
        String prompt = promptService.buildDestinyCheckPrompt(profile, action, macroEvent, random.nextDouble());
        return deepSeekService.call("You are a judge.", prompt);
    }

    /**
     * 触发随机事件
     * 根据概率生成不同类型的随机事件
     * @return 事件类型（如：黑天鹅-灾难、小挫折、平淡、小确幸、黑天鹅-奇迹）
     */
    public String triggerRandomEvent() {
        double roll = random.nextDouble();
        if (roll < 0.001) return "黑天鹅-灾难";
        if (roll > 0.999) return "黑天鹅-奇迹";
        if (roll < 0.1) return "小挫折";
        if (roll > 0.9) return "小确幸";
        return "平淡";
    }
}
