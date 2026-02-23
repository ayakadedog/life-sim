package com.example.demo.engine;

import com.example.demo.service.DeepSeekService;
import com.example.demo.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorldContext {

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private PromptService promptService;

    public String getMacroEvent(int year) {
        String prompt = promptService.buildMacroEventPrompt(year);
        try {
            return deepSeekService.call("You are a historian.", prompt);
        } catch (Exception e) {
            return "这一年宏观环境相对平稳。";
        }
    }
}
