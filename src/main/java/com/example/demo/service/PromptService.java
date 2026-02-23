package com.example.demo.service;

import com.example.demo.model.NPC;
import com.example.demo.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enterprise-grade Prompt Engineering Service.
 * Implements a Layered Architecture for Prompt Construction:
 * Layer 1: System Persona (Role Definition)
 * Layer 2: World Context (Macro Environment)
 * Layer 3: User Context (Profile State)
 * Layer 4: Task Objective (Specific Instruction)
 * Layer 5: Output Guardrails (Format, Language, Constraints)
 */
@Service
public class PromptService {

    // --- Layer 1: System Persona ---
    private static final String SYSTEM_PERSONA_NARRATOR = 
        "你是一个名为'DeepLife'的残酷人生模拟引擎。你的文风极度接近作家江南（《龙族》作者）。" +
        "你擅长用孤独、热血、哀伤的笔触描写现实。你的文字要有电影感，善用隐喻（如雨夜、樱花、高架桥、废墟）。" +
        "你关注少年的心气与成年的妥协。不要像AI那样平铺直叙，要有情绪的流动。";
    
    private static final String SYSTEM_PERSONA_PSYCHOLOGIST = 
        "你是一位深刻的心理侧写师。你能通过冰冷的数据洞察人类内心的恐惧与渴望。你不仅仅关注家庭背景，更关注个体的职业抱负、道德困境、人际关系和自我实现。";

    private static final String SYSTEM_PERSONA_HISTORIAN = 
        "你是一位专注未来的历史学家，擅长用简洁的语言概括宏观时代的洪流。";
    
    private static final String SYSTEM_PERSONA_JUDGE = 
        "你是一位绝对理性的命运裁判官。你只看概率和逻辑，不讲情面。";

    // --- Layer 5: Output Guardrails ---
    private static final String GUARDRAIL_CHINESE_ONLY = 
        "【强制约束】\n1. 必须完全使用简体中文回复。\n2. 禁止输出任何思考过程或思维链。\n3. 直接输出最终结果，不要包含任何前言后语。";
    
    private static final String GUARDRAIL_JSON_ARRAY = 
        "【格式约束】\n1. 必须返回纯 JSON 数组格式字符串。\n2. 格式示例：[\"选项A\", \"选项B\", \"选项C\"]。\n3. 不要使用 Markdown 标记（如 ```json）。\n4. 确保 JSON 格式合法。";
    
    private static final String GUARDRAIL_JSON_OBJECT = 
        "【格式约束】\n1. 必须返回纯 JSON 对象格式字符串。\n2. 不要使用 Markdown 标记。\n3. 确保 JSON 格式合法。";

    // --- Public Methods for Specific Tasks ---

    public String buildInitNarrativePrompt(UserProfile profile) {
        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            "",
            formatUserContext(profile),
            "任务：为该角色生成一段简短的开场旁白。重点描述其出身背景带来的阶级底色，以及那一点点不甘心的火苗。",
            GUARDRAIL_CHINESE_ONLY
        );
    }

    public String buildProbesPrompt(UserProfile profile) {
        return buildPrompt(
            SYSTEM_PERSONA_PSYCHOLOGIST,
            "",
            formatUserContext(profile),
            "任务：根据用户画像，生成3个直击灵魂的二选一问题（Probes）。\n" +
            "要求：\n" +
            "1. 问题必须多样化，涵盖：职业选择与野心、道德伦理困境、亲密关系的处理、自我价值的实现。\n" +
            "2. 尽量避免只问家庭背景相关的问题，要挖掘更深层的心理动机。\n" +
            "3. 结合用户填写的‘人生经历’进行个性化提问。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_ARRAY
        );
    }
    
    public String buildProbeAnalysisPrompt(UserProfile profile, Map<String, String> answers) {
        StringBuilder answersText = new StringBuilder();
        answers.forEach((q, a) -> answersText.append("问题：").append(q).append("\n回答：").append(a).append("\n"));
        
        return buildPrompt(
            SYSTEM_PERSONA_PSYCHOLOGIST,
            "用户刚刚完成了灵魂拷问。",
            formatUserContext(profile) + "\n\n【用户回答】\n" + answersText.toString(),
            "任务：分析用户的回答，提取其核心人格特质（Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism, Resilience, Ambition）和核心价值观。\n" +
            "为每个特质打分（0-100）。",
            GUARDRAIL_JSON_OBJECT + "\n返回格式示例：{\"personalityTraits\": {\"Resilience\": 80, \"Ambition\": 90}, \"coreValues\": [\"自由\", \"金钱\"]}"
        );
    }

    public String buildMacroEventPrompt(int year) {
        return buildPrompt(
            SYSTEM_PERSONA_HISTORIAN,
            "当前年份：" + year,
            "",
            "任务：推演这一年的全球宏观大事件（黑天鹅或灰犀牛）。关注经济周期、技术突变或地缘政治。一句话概括。",
            GUARDRAIL_CHINESE_ONLY
        );
    }

    public String buildYearlySimulationPrompt(UserProfile profile, String macroEvent, String destinyType, String outcomeAnalysis, String userChoice) {
        String worldContext = String.format(
            "【世界层】\n年份：%d\n宏观事件：%s", 
            2024 + (profile.getCurrentAge() - profile.getBasicInfo().getStartAge()), 
            macroEvent
        );
        
        String logicContext = String.format(
            "【判定层】\n命运类型：%s\n判定结果：%s\n用户抉择：%s", 
            destinyType, outcomeAnalysis, userChoice
        );
        
        String difficultyInstruction = "";
        if ("Hard".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为困难模式（步步惊心）。请让生活充满挑战，挫折频繁，成功来之不易。";
        } else if ("Hell".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为地狱模式（绝望求生）。请极尽残酷，每一次希望都要伴随着更大的绝望，生存是唯一目标。";
        } else if ("Easy".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为爽文模式（一路开挂）。请多给予好运、奇遇和顺遂，让主角光环闪耀。";
        } else {
            difficultyInstruction = "【模式设定】当前为常规模式（真实人生）。请保持现实主义的基调，有苦有甜，平淡中见真章。";
        }

        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            worldContext + "\n" + logicContext,
            formatUserContext(profile),
            "任务：生成本年度的‘人生结案陈词’。\n" + difficultyInstruction + "\n请必须返回 JSON 格式，包含三个字段：\n" +
            "1. event (String): 关键事件的叙事。风格要像江南，充满画面感和孤独感。\n" +
            "2. status_change (String): 身体和精神状态的变化描述。\n" +
            "3. relationship_change (String): 人际关系的微妙变迁。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_OBJECT
        );
    }
    
    public String buildSkipYearsPrompt(UserProfile profile, int years) {
        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            "时间跨度：未来 " + years + " 年",
            formatUserContext(profile),
            "任务：快速蒙太奇。概括这几年的平淡生活。请必须返回 JSON 格式，包含三个字段：\n" +
            "1. event (String): 岁月流逝的整体叙事。风格要像江南。\n" +
            "2. status_change (String): 身体状态的自然衰老。\n" +
            "3. relationship_change (String): 朋友的离散或家庭的羁绊。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_OBJECT
        );
    }

    public String buildChoicesPrompt(UserProfile profile, String currentScenario) {
        return buildPrompt(
            "你是一个精算师与编剧的结合体。",
            "当前剧情：" + currentScenario,
            formatUserContext(profile),
            "任务：基于当前处境，提供3个具体的下一步行动选项。\n要求：\n1. 激进型（高风险高回报）\n2. 保守型（稳扎稳打）\n3. 情感/社交型（非功利性）",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_ARRAY
        );
    }

    public String buildNPCEvolutionPrompt(NPC npc, UserProfile profile) {
        return buildPrompt(
            "你是一个社会关系模拟器。",
            "玩家当前处境：" + profile.getCurrentScenario(),
            String.format("NPC资料：姓名=%s, 关系=%s, 年龄=%d, 状态=%s", npc.getName(), npc.getRelation(), npc.getAge(), npc.getStatus()),
            "任务：推演该NPC本年度的生活变故或与玩家的互动。",
            GUARDRAIL_CHINESE_ONLY + "\n限制：20字以内。"
        );
    }
    
    public String buildDestinyCheckPrompt(UserProfile profile, String action, String macroEvent, double roll) {
        String difficultyInstruction = "";
        if ("Hard".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为困难模式。判定标准极其严苛，非大成功即为失败。";
        } else if ("Hell".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为地狱模式。判定标准近乎绝望，除非掷出极高值，否则一律判定为灾难。";
        } else if ("Easy".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为爽文模式。判定标准宽松，容易获得成功，甚至意外之喜。";
        } else {
            difficultyInstruction = "【模式设定】当前为常规模式。判定标准基于概率和逻辑，公平公正。";
        }
        
        return buildPrompt(
            SYSTEM_PERSONA_JUDGE,
            "宏观事件：" + macroEvent + "\n随机判定值(0-1)：" + roll,
            formatUserContext(profile),
            "用户试图采取行动：" + action + "\n" + difficultyInstruction + "\n任务：基于用户能力值、宏观环境和随机值，判定行动结果（成功/失败/大成功/大失败）。\n请给出简短的判定理由。",
            GUARDRAIL_CHINESE_ONLY
        );
    }

    // --- Helper Methods ---

    private String buildPrompt(String layer1, String layer2, String layer3, String layer4, String layer5) {
        StringBuilder sb = new StringBuilder();
        if (!layer1.isEmpty()) sb.append("### Role ###\n").append(layer1).append("\n\n");
        if (!layer2.isEmpty()) sb.append("### World Context ###\n").append(layer2).append("\n\n");
        if (!layer3.isEmpty()) sb.append("### User Profile ###\n").append(layer3).append("\n\n");
        sb.append("### Task ###\n").append(layer4).append("\n\n");
        sb.append("### Constraints ###\n").append(layer5);
        return sb.toString();
    }

    private String formatUserContext(UserProfile profile) {
        String parentsStatus = "未知";
        String familyAssets = "未知";
        String fatherProfession = "未知";
        String motherProfession = "未知";

        if (profile.getFamilyBackground() != null) {
            parentsStatus = profile.getFamilyBackground().getParentsStatus() != null ? profile.getFamilyBackground().getParentsStatus() : "未知";
            familyAssets = profile.getFamilyBackground().getFamilyAssets() != null ? profile.getFamilyBackground().getFamilyAssets() : "未知";
            fatherProfession = profile.getFamilyBackground().getFatherProfession() != null ? profile.getFamilyBackground().getFatherProfession() : "未知";
            motherProfession = profile.getFamilyBackground().getMotherProfession() != null ? profile.getFamilyBackground().getMotherProfession() : "未知";
        }

        String familyInfo = String.format(
            "父母状况: %s, 家庭资产: %s, 父亲职业: %s, 母亲职业: %s",
            parentsStatus, familyAssets, fatherProfession, motherProfession
        );
        
        return String.format(
            "- 游戏模式：%s\n" + 
            "- 基本信息：姓名 %s, 年龄 %d, 学历 %s, 职业 %s, 坐标 %s\n" +
            "- 人生经历（重要）：%s\n" +
            "- 经济状况：存款 %.0f, 负债 %.0f\n" +
            "- 身体状态：精力值 %d%%\n" +
            "- 家庭背景：%s\n" +
            "- 核心特质：%s\n" +
            "- 核心价值观：%s",
            profile.getDifficulty() != null ? profile.getDifficulty() : "Normal",
            profile.getBasicInfo().getName(),
            profile.getCurrentAge(),
            profile.getBasicInfo().getEducationLevel(),
            profile.getBasicInfo().getProfession(),
            profile.getBasicInfo().getLocation(),
            profile.getBasicInfo().getLifeExperiences() != null ? profile.getBasicInfo().getLifeExperiences() : "无",
            profile.getEconomicStatus().getSavings(),
            profile.getEconomicStatus().getDebt(),
            profile.getHealthStatus().getEnergyLevel(),
            familyInfo,
            profile.getPersonalityTraits() != null ? profile.getPersonalityTraits().toString() : "{}",
            profile.getCoreValues() != null ? profile.getCoreValues().toString() : "[]"
        );
    }
}
