package com.example.demo.service;

import com.example.demo.engine.DestinyEngine;
import com.example.demo.engine.WorldContext;
import com.example.demo.model.*;
import com.example.demo.repository.CharacterTemplateRepository;
import com.example.demo.repository.GameInstanceRepository;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimulationService {

    @Autowired
    private UserProfileRepository userProfileRepository;
    
    @Autowired
    private GameInstanceRepository gameInstanceRepository;
    
    @Autowired
    private CharacterTemplateRepository characterTemplateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private DestinyEngine destinyEngine;
    
    @Autowired
    private WorldContext worldContext;
    
    @Autowired
    private NPCService npcService;
    
    @Autowired
    private PromptService promptService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Template Management ---
    
    public CharacterTemplate createTemplate(Long userId, UserProfile profile) {
        CharacterTemplate template = new CharacterTemplate();
        template.setUserId(userId);
        template.setName(profile.getBasicInfo().getName());
        try {
            template.setBasicInfo(objectMapper.writeValueAsString(profile.getBasicInfo()));
            template.setFamilyBackground(objectMapper.writeValueAsString(profile.getFamilyBackground()));
            template.setInitialAttributes(objectMapper.writeValueAsString(profile.getEconomicStatus())); // Simplified for now
        } catch (Exception e) {
            e.printStackTrace();
        }
        return characterTemplateRepository.save(template);
    }
    
    public List<CharacterTemplate> getUserTemplates(Long userId) {
        return characterTemplateRepository.findByUserId(userId);
    }

    // --- Game Instance Management ---
    
    public GameInstance startGame(Long userId, Long templateId, UserProfile initialProfile) {
        GameInstance instance = new GameInstance();
        instance.setUserId(userId);
        instance.setTemplateId(templateId);
        
        if (initialProfile.getId() != null) {
            // Check if profile already exists to avoid duplicate entry error
            UserProfile existingProfile = userProfileRepository.findById(initialProfile.getId()).orElse(null);
            if (existingProfile != null) {
                // Use the managed entity
                instance.setUserProfile(existingProfile);
            } else {
                // ID provided but not found (unlikely), treat as new
                instance.setUserProfile(initialProfile);
            }
        } else {
            initialProfile.setId(UUID.randomUUID().toString());
            initialProfile.setCurrentAge(initialProfile.getBasicInfo().getStartAge());
            instance.setUserProfile(initialProfile);
        }
        
        return gameInstanceRepository.save(instance);
    }
    
    public List<GameInstance> getUserHistory(Long userId) {
        return gameInstanceRepository.findByUserIdOrderByLastUpdateTimeDesc(userId);
    }
    
    public GameInstance getGameInstance(Long instanceId) {
        return gameInstanceRepository.findById(instanceId).orElse(null);
    }

    // --- Init Step 1: Create Basic Profile (Refactored for new flow) ---
    // Kept for compatibility but now we should use startGame
    public UserProfile createProfile(UserProfile profile) {
        if (profile.getId() == null) profile.setId(UUID.randomUUID().toString());
        profile.setCurrentAge(profile.getBasicInfo().getStartAge());
        return userProfileRepository.save(profile);
    }

    // --- Init Step 2: Analyze Answers & Start ---
    public UserProfile analyzeProbesAndStart(String profileId, Map<String, String> answers) {
        UserProfile profile = userProfileRepository.findById(profileId).orElseThrow();
        
        // 1. Analyze Personality
        String analysisPrompt = promptService.buildProbeAnalysisPrompt(profile, answers);
        String analysisJson = deepSeekService.callExpectingJson("You are a psychologist.", analysisPrompt);
        
        try {
            JSONObject analysis = new JSONObject(analysisJson);
            if (analysis.has("personalityTraits")) {
                JSONObject traits = analysis.getJSONObject("personalityTraits");
                for (String key : traits.keySet()) {
                    profile.getPersonalityTraits().put(key, traits.getInt(key));
                }
            }
            if (analysis.has("coreValues")) {
                JSONArray values = analysis.getJSONArray("coreValues");
                for (int i = 0; i < values.length(); i++) {
                    profile.getCoreValues().add(values.getString(i));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse personality analysis: " + e.getMessage());
            // Set default if failed
            profile.getPersonalityTraits().put("Openness", 50);
            profile.getPersonalityTraits().put("Resilience", 50);
        }

        // 2. Generate Opening Narrative
        String prompt = promptService.buildInitNarrativePrompt(profile);
        String opening = deepSeekService.callExpectingJson("You are a narrator.", prompt);
        
        // Ensure consistent JSON structure for the opening as well
        try {
            JSONObject openingJson = new JSONObject(opening);
            if (!openingJson.has("status_change")) openingJson.put("status_change", "一切如常");
            if (!openingJson.has("relationship_change")) openingJson.put("relationship_change", "无明显变化");
            profile.setCurrentScenario(openingJson.toString());
        } catch (Exception e) {
            // Fallback
            profile.setCurrentScenario(new JSONObject()
                .put("event", opening)
                .put("status_change", "一切如常")
                .put("relationship_change", "无明显变化")
                .toString());
        }
        
        // 3. Initial Choices
        profile.setAvailableChoices(generateDynamicChoices(profile, "游戏开始"));

        // 4. Init NPCs if empty
        if (profile.getNpcs() == null || profile.getNpcs().isEmpty()) {
            initNPCs(profile);
        }

        return userProfileRepository.save(profile);
    }

    private void initNPCs(UserProfile profile) {
        if (profile.getNpcs() == null) {
            profile.setNpcs(new ArrayList<>());
        } else {
            profile.getNpcs().clear();
        }

        NPC father = new NPC();
        father.setName("父亲");
        father.setRelation("FATHER");
        father.setAge(profile.getCurrentAge() + 25);
        father.setStatus("HEALTHY");
        father.setIntimacy(80);
        father.setCurrentSituation("依然在为家庭操劳，偶尔抱怨腰疼。");
        profile.getNpcs().add(father);

        NPC mother = new NPC();
        mother.setName("母亲");
        mother.setRelation("MOTHER");
        mother.setAge(profile.getCurrentAge() + 24);
        mother.setStatus("HEALTHY");
        mother.setIntimacy(85);
        mother.setCurrentSituation("每天操持家务，最担心你的终身大事。");
        profile.getNpcs().add(mother);
    }

    // --- Dynamic Probes ---
    public List<String> generateProbes(UserProfile profile) {
        String prompt = promptService.buildProbesPrompt(profile);
        String response = deepSeekService.callExpectingJson("You are a psychologist.", prompt);
        
        List<String> probes = new ArrayList<>();
        try {
            if (response.startsWith("[")) {
                 JSONArray jsonArray = new JSONArray(response);
                 for(int i=0; i<jsonArray.length(); i++) {
                     probes.add(jsonArray.getString(i));
                 }
            } else {
                 // Try to parse if it's wrapped in a JSON object
                 JSONObject obj = new JSONObject(response);
                 if (obj.has("probes")) {
                     JSONArray jsonArray = obj.getJSONArray("probes");
                     for(int i=0; i<jsonArray.length(); i++) {
                         probes.add(jsonArray.getString(i));
                     }
                 }
            }
        } catch (Exception e) {
            probes.add("如果你必须在金钱和自由之间二选一，你会选什么？为什么？");
            probes.add("现在的你是在追求梦想，还是在逃避现实？");
            probes.add("你认为什么样的人生才算没有虚度？");
        }
        if (probes.isEmpty()) {
             probes.add("如果你必须在金钱和自由之间二选一，你会选什么？为什么？");
             probes.add("现在的你是在追求梦想，还是在逃避现实？");
             probes.add("你认为什么样的人生才算没有虚度？");
        }
        return probes;
    }

    // --- Core Loop: Simulate Year ---
    public UserProfile simulateYear(String profileId, String userChoice) {
        UserProfile profile = userProfileRepository.findById(profileId).orElseThrow();
        int currentYear = 2024 + (profile.getCurrentAge() - profile.getBasicInfo().getStartAge());
        
        // 1. Macro Event
        String macroEvent = worldContext.getMacroEvent(currentYear);
        
        // 2. Random/Destiny Event & Outcome
        String destinyType = destinyEngine.triggerRandomEvent();
        String outcomeAnalysis = destinyEngine.determineOutcome(profile, userChoice, macroEvent);
        
        // 3. NPC Evolution
        npcService.evolveNPCs(profile);
        
        // 4. LLM Narrative Generation (Requesting JSON)
        String prompt = promptService.buildYearlySimulationPrompt(profile, macroEvent, destinyType, outcomeAnalysis, userChoice);
        String response = deepSeekService.callExpectingJson("You are a narrator.", prompt);
        
        String narrativeText = "";
        
        try {
            JSONObject json = new JSONObject(response);
            narrativeText = json.optString("event", "岁月无声，生活继续。");
            
            // Ensure status_change and relationship_change are present
            if (!json.has("status_change")) json.put("status_change", "无明显变化");
            if (!json.has("relationship_change")) json.put("relationship_change", "一切如常");
            
            profile.setCurrentScenario(json.toString()); 
            
        } catch (Exception e) {
            // Fallback to raw text if JSON parse fails
            narrativeText = response;
            profile.setCurrentScenario(new JSONObject()
                .put("event", response)
                .put("status_change", "无明显变化")
                .put("relationship_change", "一切如常")
                .toString());
        }
        
        // 5. Update Profile
        profile.setCurrentAge(profile.getCurrentAge() + 1);
        
        LifeHistory history = new LifeHistory();
        history.setAge(profile.getCurrentAge());
        history.setEventDescription(profile.getCurrentScenario()); // Store the JSON string
        history.setEventType(destinyType);
        profile.getLifeHistory().add(history);
        
        profile.getHealthStatus().setEnergyLevel(Math.max(0, profile.getHealthStatus().getEnergyLevel() - 1));
        
        // 6. Generate Next Choices
        profile.setAvailableChoices(generateDynamicChoices(profile, narrativeText));
        
        // Update GameInstance lastUpdateTime if linked (not strictly enforced here but good practice)
        // We could do a reverse lookup if needed, but for now just saving profile is enough for data
        
        return userProfileRepository.save(profile);
    }
    
    // --- Skip Years ---
    public UserProfile skipYears(String profileId, int years) {
        UserProfile profile = userProfileRepository.findById(profileId).orElseThrow();
        
        for(int i=0; i<years; i++) {
             profile.setCurrentAge(profile.getCurrentAge() + 1);
             profile.getHealthStatus().setEnergyLevel(Math.max(0, profile.getHealthStatus().getEnergyLevel() - 1));
        }
        
        String prompt = promptService.buildSkipYearsPrompt(profile, years);
        String response = deepSeekService.callExpectingJson("You are a narrator.", prompt);
        
        try {
             // Ensure consistent JSON format even for skips
             JSONObject json = new JSONObject(response);
             profile.setCurrentScenario(json.toString());
        } catch (Exception e) {
             profile.setCurrentScenario(new JSONObject()
                .put("event", response)
                .put("status_change", "岁月流逝")
                .put("relationship_change", "故人渐远")
                .toString());
        }

        String narrativeContext = "";
        try {
            narrativeContext = new JSONObject(profile.getCurrentScenario()).getString("event");
        } catch(Exception e) {
            narrativeContext = "时光飞逝";
        }

        profile.setAvailableChoices(generateDynamicChoices(profile, narrativeContext));
        
        return userProfileRepository.save(profile);
    }

    private List<String> generateDynamicChoices(UserProfile profile, String context) {
        String prompt = promptService.buildChoicesPrompt(profile, context);
        String response = deepSeekService.callExpectingJson("You are a game designer.", prompt);
        
        List<String> choices = new ArrayList<>();
        try {
            if (response.startsWith("[")) {
                 JSONArray jsonArray = new JSONArray(response);
                 for(int i=0; i<jsonArray.length(); i++) {
                     choices.add(jsonArray.getString(i));
                 }
            }
        } catch (Exception e) {
            // Fallback
        }
        if (choices.isEmpty()) {
            choices.add("继续专注于工作");
            choices.add("多花时间陪陪家人");
            choices.add("尝试发展副业");
        }
        return choices;
    }
    
    // --- Legacy ---
    public UserProfile createLegacy(String parentId) {
        UserProfile parent = userProfileRepository.findById(parentId).orElseThrow();
        UserProfile child = new UserProfile();
        child.setId(UUID.randomUUID().toString());
        child.setGeneration(parent.getGeneration() + 1);
        child.setParentProfileId(parent.getId());
        
        // Inheritance Logic
        child.getBasicInfo().setName(parent.getBasicInfo().getName() + "的孩子");
        child.getEconomicStatus().setSavings(parent.getEconomicStatus().getSavings() * 0.8); 
        child.getBasicInfo().setStartAge(0); 
        child.getBasicInfo().setLocation(parent.getBasicInfo().getLocation());
        
        // Init child NPCs
        List<NPC> childNpcs = new ArrayList<>();
        NPC oldParent = new NPC();
        oldParent.setName(parent.getBasicInfo().getName());
        oldParent.setRelation("PARENT");
        oldParent.setAge(parent.getCurrentAge());
        oldParent.setStatus("RETIRED");
        childNpcs.add(oldParent);
        child.setNpcs(childNpcs);
        
        return userProfileRepository.save(child);
    }
}
