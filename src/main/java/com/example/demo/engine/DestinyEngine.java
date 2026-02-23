package com.example.demo.engine;

import com.example.demo.model.UserProfile;
import com.example.demo.service.DeepSeekService;
import com.example.demo.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class DestinyEngine {

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private PromptService promptService;

    private final Random random = new Random();

    public String determineOutcome(UserProfile profile, String action, String macroEvent) {
        String prompt = promptService.buildDestinyCheckPrompt(profile, action, macroEvent, random.nextDouble());
        return deepSeekService.call("You are a judge.", prompt);
    }

    public String triggerRandomEvent() {
        double roll = random.nextDouble();
        if (roll < 0.001) return "黑天鹅-灾难";
        if (roll > 0.999) return "黑天鹅-奇迹";
        if (roll < 0.1) return "小挫折";
        if (roll > 0.9) return "小确幸";
        return "平淡";
    }
}
