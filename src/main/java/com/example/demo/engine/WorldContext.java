package com.example.demo.engine;

import com.example.demo.service.DeepSeekService;
import com.example.demo.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 世界环境上下文
 * 负责生成和管理游戏中的宏观历史背景和事件
 */
@Component
public class WorldContext {

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private PromptService promptService;

    /**
     * 获取指定年份的宏观事件
     * 调用 AI 根据年份生成当年的历史/社会大背景
     * @param year 年份
     * @return 宏观事件描述
     */
    public String getMacroEvent(int year) {
        String prompt = promptService.buildMacroEventPrompt(year);
        try {
            return deepSeekService.call("You are a historian.", prompt);
        } catch (Exception e) {
            return "这一年宏观环境相对平稳。";
        }
    }
}
