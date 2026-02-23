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

/**
 * 模拟服务
 * 核心业务逻辑类，协调各个组件（Engine, Prompt, NPC, Persistence）完成人生模拟流程
 */
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
    
    /**
     * 创建角色模板
     * 将当前用户档案保存为模板，供以后使用
     * @param userId 用户ID
     * @param profile 用户档案
     * @return 创建的模板
     */
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
    
    /**
     * 获取用户的所有角色模板
     * @param userId 用户ID
     * @return 模板列表
     */
    public List<CharacterTemplate> getUserTemplates(Long userId) {
        return characterTemplateRepository.findByUserId(userId);
    }

    // --- Game Instance Management ---
    
    /**
     * 开始新游戏实例
     * @param userId 用户ID
     * @param templateId 模板ID
     * @param initialProfile 初始档案
     * @return 创建的游戏实例
     */
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
    
    /**
     * 获取用户的游戏历史
     * @param userId 用户ID
     * @return 游戏实例列表，按最后更新时间降序排列
     */
    public List<GameInstance> getUserHistory(Long userId) {
        return gameInstanceRepository.findByUserIdOrderByLastUpdateTimeDesc(userId);
    }
    
    /**
     * 获取游戏实例详情
     * @param instanceId 实例ID
     * @return 游戏实例
     */
    public GameInstance getGameInstance(Long instanceId) {
        return gameInstanceRepository.findById(instanceId).orElse(null);
    }

    // --- Init Step 1: Create Basic Profile (Refactored for new flow) ---
    // Kept for compatibility but now we should use startGame
    /**
     * 创建基本档案
     * 初始化一个新的用户档案，分配UUID和初始年龄
     * @param profile 用户档案
     * @return 保存后的用户档案
     */
    public UserProfile createProfile(UserProfile profile) {
        if (profile.getId() == null) profile.setId(UUID.randomUUID().toString());
        profile.setCurrentAge(profile.getBasicInfo().getStartAge());
        return userProfileRepository.save(profile);
    }

    // --- Init Step 2: Analyze Answers & Start ---
    /**
     * 分析探测问题答案并开始模拟
     * 1. 调用 AI 分析用户回答，提取人格特质和核心价值观
     * 2. 生成开场旁白
     * 3. 生成初始选择
     * 4. 初始化 NPC
     * @param profileId 档案ID
     * @param answers 用户回答
     * @return 更新后的用户档案
     */
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
            // Check if response is JSON Object
            if (opening.trim().startsWith("{")) {
                JSONObject openingJson = new JSONObject(opening);
                if (!openingJson.has("status_change")) openingJson.put("status_change", "一切如常");
                if (!openingJson.has("relationship_change")) openingJson.put("relationship_change", "无明显变化");
                if (!openingJson.has("event") && openingJson.length() > 0) {
                     // If no event field but has other fields, maybe the whole object is the content?
                     // Or maybe keys are different. Let's try to find a long string value.
                     // But for now, let's assume 'event' is required or we take the whole string.
                     if (openingJson.has("narrative")) {
                         openingJson.put("event", openingJson.getString("narrative"));
                     } else {
                         openingJson.put("event", openingJson.toString());
                     }
                }
                profile.setCurrentScenario(openingJson.toString());
            } else {
                // If not JSON object, treat as raw text
                 profile.setCurrentScenario(new JSONObject()
                    .put("event", opening)
                    .put("status_change", "一切如常")
                    .put("relationship_change", "无明显变化")
                    .toString());
            }
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

    /**
     * 初始化 NPC
     * 创建默认的父母 NPC
     * @param profile 用户档案
     */
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
    /**
     * 生成探测问题
     * 调用 AI 根据用户档案生成一组探测问题，用于了解用户的价值观和倾向
     * @param profile 用户档案
     * @return 探测问题列表
     */
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
    /**
     * 模拟下一年
     * 核心游戏循环：
     * 1. 确定宏观事件
     * 2. 触发随机事件和命运判定
     * 3. 演化 NPC 状态
     * 4. 生成 AI 叙事（剧情、状态变化、关系变化）
     * 5. 更新档案状态（年龄、精力、历史记录）
     * 6. 生成下一步选择
     * 7. 更新长期记忆
     * @param profileId 档案ID
     * @param userChoice 用户的上一步选择
     * @return 更新后的用户档案
     */
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
        
        // 7. Update Long-Term Memory (Rolling Memory)
        updateLongTermMemory(profile, narrativeText);
        
        // Update GameInstance lastUpdateTime if linked (not strictly enforced here but good practice)
        // We could do a reverse lookup if needed, but for now just saving profile is enough for data
        
        return userProfileRepository.save(profile);
    }
    
    // --- Skip Years ---
    /**
     * 跳过多年
     * 快速推进时间，生成概要性的叙事
     * @param profileId 档案ID
     * @param years 跳过的年数
     * @return 更新后的用户档案
     */
    public UserProfile skipYears(String profileId, int years) {
        UserProfile profile = userProfileRepository.findById(profileId).orElseThrow();
        
        for(int i=0; i<years; i++) {
             profile.setCurrentAge(profile.getCurrentAge() + 1);
             profile.getHealthStatus().setEnergyLevel(Math.max(0, profile.getHealthStatus().getEnergyLevel() - 1));
        }
        
        String prompt = promptService.buildSkipYearsPrompt(profile, years);
        String response = deepSeekService.callExpectingJson("You are a narrator.", prompt);
        
        String narrativeText = "";
        try {
            JSONObject json = new JSONObject(response);
            narrativeText = json.optString("event", "时光飞逝。");
            
            if (!json.has("status_change")) json.put("status_change", "岁月留痕");
            if (!json.has("relationship_change")) json.put("relationship_change", "故人渐远");
            
            profile.setCurrentScenario(json.toString());
        } catch (Exception e) {
            narrativeText = response;
            profile.setCurrentScenario(new JSONObject()
                .put("event", response)
                .put("status_change", "岁月留痕")
                .put("relationship_change", "故人渐远")
                .toString());
        }
        
        LifeHistory history = new LifeHistory();
        history.setAge(profile.getCurrentAge());
        history.setEventDescription(profile.getCurrentScenario());
        history.setEventType("跳过");
        profile.getLifeHistory().add(history);
        
        profile.setAvailableChoices(generateDynamicChoices(profile, narrativeText));
        
        // Update Memory for skipped years
        updateLongTermMemory(profile, narrativeText);
        
        return userProfileRepository.save(profile);
    }
    
    /**
     * 更新长期记忆
     * 调用 AI 将新的经历压缩并合并到长期记忆中
     * @param profile 用户档案
     * @param newEvent 新的经历
     */
    private void updateLongTermMemory(UserProfile profile, String newEvent) {
        try {
            // Async or sync? For now sync to ensure consistency
            String memoryPrompt = promptService.buildMemoryConsolidationPrompt(profile, newEvent);
            String consolidatedMemory = deepSeekService.call("You are a biographer.", memoryPrompt);
            // Simple cleanup if AI returns markdown
            if (consolidatedMemory != null && consolidatedMemory.contains("```")) {
                consolidatedMemory = consolidatedMemory.replaceAll("```", "").trim();
            }
            if (consolidatedMemory != null) {
                profile.setLongTermMemory(consolidatedMemory);
            }
        } catch (Exception e) {
            System.err.println("Failed to update memory: " + e.getMessage());
        }
    }

    /**
     * 生成动态选择
     * 根据当前剧情和档案，生成下一步的行动选项
     * @param profile 用户档案
     * @param context 当前剧情上下文
     * @return 选项列表
     */
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
    /**
     * 创建遗产/下一代
     * 继承父辈的资产和部分属性，开始新的人生
     * @param parentId 父辈档案ID
     * @return 子代用户档案
     */
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
