package com.example.demo.service;

import com.example.demo.model.NPC;
import com.example.demo.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NPCService {

    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private PromptService promptService;

    public void evolveNPCs(UserProfile profile) {
        List<NPC> npcs = profile.getNpcs();
        if (npcs == null || npcs.isEmpty()) return;

        for (NPC npc : npcs) {
            npc.setAge(npc.getAge() + 1);
            
            String prompt = promptService.buildNPCEvolutionPrompt(npc, profile);
            String update = deepSeekService.call("You are a NPC engine.", prompt);
            
            npc.setCurrentSituation(update);
            
            if (update.contains("病") || update.contains("住院")) {
                npc.setStatus("SICK");
            } else if (update.contains("去世") || update.contains("死")) {
                npc.setStatus("DEAD");
            }
        }
    }
}
