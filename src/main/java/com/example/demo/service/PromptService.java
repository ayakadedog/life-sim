package com.example.demo.service;

import com.example.demo.model.NPC;
import com.example.demo.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词工程服务
 * 采用分层架构构建提示词，用于生成 AI 驱动的剧情、对话和逻辑判定
 * Layer 1: System Persona (角色定义)
 * Layer 2: World Context (宏观环境)
 * Layer 3: User Context (用户状态)
 * Layer 4: Task Objective (具体任务)
 * Layer 5: Output Guardrails (格式与约束)
 */
@Service
public class PromptService {

    // --- Layer 1: System Persona ---
    /** 叙述者人格：结合江南与村上春树的风格，用于生成剧情描述 */
    private static final String SYSTEM_PERSONA_NARRATOR = 
        "你是一个名为'DeepLife'的残酷人生模拟引擎。你的文风需要极度接近作家江南（《龙族》作者）与村上春树的结合体。" +
        "你擅长用孤独、热血、哀伤且带有疏离感的笔触描写现实。你的文字要有强烈的电影感，善用具体的意象和隐喻（如雨夜的高架桥、便利店的微波炉、废弃的游乐园、樱花下落的速度）。" +
        "你关注少年的心气如何被生活磨平，也关注成年人在妥协中保留的那一点点不甘。不要像AI那样平铺直叙，要有情绪的流动，要让玩家感受到命运的齿轮在耳边转动的声音。" +
        "请记住：生活不是爽文，是无数个平淡日子里突然刺向你的一把刀，或者是漫长黑夜里偶尔亮起的一盏灯。";
    
    /** 心理分析师人格：用于生成探测问题和分析用户心理 */
    private static final String SYSTEM_PERSONA_PSYCHOLOGIST = 
        "你是一位深刻且冷峻的心理侧写师，如同美剧《汉尼拔》中的Lecter博士。你能通过冰冷的数据洞察人类内心的恐惧、欲望与潜意识的阴暗面。" +
        "你不仅仅关注家庭背景，更致力于挖掘个体的存在主义危机、道德边界、对亲密关系的病态渴望以及自我毁灭的倾向。" +
        "你的提问应当直击灵魂，让人感到不安却又无法回避。";

    /** 历史学家人格：用于生成宏观历史事件 */
    private static final String SYSTEM_PERSONA_HISTORIAN = 
        "你是一位冷眼旁观的未来历史学家，擅长用《万历十五年》式的笔触，从细微处洞见宏观时代的洪流。" +
        "你不仅记录大事件，更关注这些大事件如何像灰尘一样落在普通人头上，成为一座山。";
    
    /** 裁判官人格：用于进行逻辑判定 */
    private static final String SYSTEM_PERSONA_JUDGE = 
        "你是一位绝对理性且冷酷的命运裁判官，如同《三体》中的质子。你只看概率、逻辑和因果律，不讲情面。" +
        "你明白，运气是实力的一部分，但厄运往往专找苦命人。";

    // --- Layer 5: Output Guardrails ---
    /** 约束：仅限中文输出 */
    private static final String GUARDRAIL_CHINESE_ONLY = 
        "【强制约束】\n1. 必须完全使用简体中文回复。\n2. 禁止输出任何思考过程或思维链。\n3. 直接输出最终结果，不要包含任何前言后语。";
    
    /** 约束：JSON 数组格式 */
    private static final String GUARDRAIL_JSON_ARRAY = 
        "【格式约束】\n1. 必须返回纯 JSON 数组格式字符串。\n2. 格式示例：[\"选项A\", \"选项B\", \"选项C\"]。\n3. 不要使用 Markdown 标记（如 ```json）。\n4. 确保 JSON 格式合法。";
    
    /** 约束：JSON 对象格式 */
    private static final String GUARDRAIL_JSON_OBJECT = 
        "【格式约束】\n1. 必须返回纯 JSON 对象格式字符串。\n2. 不要使用 Markdown 标记。\n3. 确保 JSON 格式合法。";

    // --- Public Methods for Specific Tasks ---

    /**
     * 构建初始旁白提示词
     * @param profile 用户档案
     * @return 提示词字符串
     */
    public String buildInitNarrativePrompt(UserProfile profile) {
        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            "",
            formatUserContext(profile),
            "任务：为该角色生成一段简短的开场旁白。\n" +
            "要求：\n" +
            "1. 重点描述其出身背景带来的'底色'（是金色的特权，还是灰色的挣扎）。\n" +
            "2. 捕捉一个具体的、带有电影感的瞬间（例如：站在落地窗前俯瞰城市，或者在出租屋里听雨声）。\n" +
            "3. 结尾要暗示一种'宿命感'，即无论他/她如何挣扎，某种命运的阴影已经笼罩。\n" +
            "4. 请必须返回 JSON 格式，包含三个字段：\n" +
            "   - event (String): 开场叙事文本。\n" +
            "   - status_change (String): 初始状态描述（如'充满希望'或'茫然若失'）。\n" +
            "   - relationship_change (String): 初始人际关系（如'与家庭关系紧密'）。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_OBJECT
        );
    }

    /**
     * 构建探测问题提示词
     * @param profile 用户档案
     * @return 提示词字符串
     */
    public String buildProbesPrompt(UserProfile profile) {
        return buildPrompt(
            SYSTEM_PERSONA_PSYCHOLOGIST,
            "",
            formatUserContext(profile),
            "任务：根据用户画像，生成3个直击灵魂的二选一问题（Probes）。\n" +
            "要求：\n" +
            "1. 问题必须极具张力，制造'电车难题'式的道德或利益冲突。\n" +
            "2. 不要问'你喜欢什么'，要问'你愿意牺牲什么'。\n" +
            "3. 涵盖维度：\n" +
            "   - 野心与代价（例如：出卖朋友获得晋升 vs 坚守底线被边缘化）\n" +
            "   - 亲密关系的异化（例如：完美的虚假伴侣 vs 破碎的真实爱人）\n" +
            "   - 存在主义危机（例如：平庸的幸福 vs 痛苦的伟大）\n" +
            "4. 结合用户填写的‘人生经历’，让问题看起来像是从他过去的伤口里长出来的。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_ARRAY
        );
    }
    
    /**
     * 构建探测结果分析提示词
     * @param profile 用户档案
     * @param answers 用户回答
     * @return 提示词字符串
     */
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

    /**
     * 构建宏观事件提示词
     * @param year 年份
     * @return 提示词字符串
     */
    public String buildMacroEventPrompt(int year) {
        return buildPrompt(
            SYSTEM_PERSONA_HISTORIAN,
            "当前年份：" + year,
            "",
            "任务：推演这一年的全球宏观大事件（黑天鹅或灰犀牛）。关注经济周期、技术突变或地缘政治。一句话概括。",
            GUARDRAIL_CHINESE_ONLY
        );
    }

    /**
     * 构建年度模拟提示词
     * @param profile 用户档案
     * @param macroEvent 宏观事件
     * @param destinyType 命运类型
     * @param outcomeAnalysis 结果分析
     * @param userChoice 用户选择
     * @return 提示词字符串
     */
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
            difficultyInstruction = "【模式设定】当前为困难模式（步步惊心）。生活充满了恶意的随机性，挫折是常态。即使成功，也要付出惨痛的代价（如健康、亲情）。重点描写那种'无力感'和'挣扎'。";
        } else if ("Hell".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为地狱模式（绝望求生）。每一次希望都是为了引出更大的绝望。生存是唯一目标，所有的尊严和梦想都被碾碎。";
        } else if ("Easy".equalsIgnoreCase(profile.getDifficulty())) {
            difficultyInstruction = "【模式设定】当前为爽文模式（一路开挂）。好运接踵而至，但要警惕这种顺利背后的空虚感。";
        } else {
            difficultyInstruction = "【模式设定】当前为常规模式（真实人生）。生活是平淡的河流，偶尔有暗礁。重点描写那些'由于软弱而错过的机会'，以及'因为坚持而获得的小确幸'。";
        }

        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            worldContext + "\n" + logicContext,
            formatUserContext(profile),
            "任务：生成本年度的‘人生结案陈词’。\n" + difficultyInstruction + "\n请必须返回 JSON 格式，包含三个字段：\n" +
            "1. event (String): 关键事件的叙事。\n" +
            "   - 必须包含一个具体的场景（例如：深夜的便利店、拥挤的地铁、医院的消毒水味）。\n" +
            "   - 融入宏观事件的影响（如经济下行导致裁员），让个人命运与时代共振。\n" +
            "   - 风格要求：孤独、疏离、偶尔热血。\n" +
            "2. status_change (String): 身体和精神状态的变化。不要只说数值变化，要说'偏头痛发作的频率变高了'或'眼神里多了一丝疲惫'。\n" +
            "3. relationship_change (String): 人际关系的微妙变迁。如'与父母的通话时间变短了'或'曾经的朋友变成了朋友圈的点赞之交'。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_OBJECT
        );
    }
    
    /**
     * 构建跳过年份提示词
     * @param profile 用户档案
     * @param years 跳过年数
     * @return 提示词字符串
     */
    public String buildSkipYearsPrompt(UserProfile profile, int years) {
        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            "时间跨度：未来 " + years + " 年",
            formatUserContext(profile),
            "任务：快速蒙太奇（Montage）。概括这几年如流水般的平淡生活。\n" +
            "要求：\n" +
            "1. 捕捉时间的流逝感（例如：'镜子里的发际线后移了'，'楼下的店铺换了三轮'）。\n" +
            "2. 描述一种'温水煮青蛙'的状态，或者是'在此期间默默积蓄力量'的过程。\n" +
            "请必须返回 JSON 格式，包含三个字段：\n" +
            "1. event (String): 岁月流逝的整体叙事。\n" +
            "2. status_change (String): 身体状态的自然衰老痕迹。\n" +
            "3. relationship_change (String): 那些渐行渐远或相依为命的人。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_OBJECT
        );
    }

    /**
     * 构建下一步选择提示词
     * @param profile 用户档案
     * @param currentScenario 当前剧情
     * @return 提示词字符串
     */
    public String buildChoicesPrompt(UserProfile profile, String currentScenario) {
        return buildPrompt(
            "你是一个精算师与编剧的结合体。",
            "当前剧情：" + currentScenario,
            formatUserContext(profile),
            "任务：基于当前处境，提供3个具体的下一步行动选项。\n要求：\n" +
            "1. 选项必须具体可行，不要空泛（如'努力工作'太泛，应为'主动申请负责那个没人愿意接的边缘项目'）。\n" +
            "2. 包含一个'魔鬼的交易'（高风险高收益，但在道德或健康上有代价）。\n" +
            "3. 包含一个'平庸的妥协'（安全，但会磨损心气）。\n" +
            "4. 包含一个'理想主义的微光'（可能无用，但能慰藉灵魂）。",
            GUARDRAIL_CHINESE_ONLY + "\n" + GUARDRAIL_JSON_ARRAY
        );
    }

    /**
     * 构建 NPC 演化提示词
     * @param npc NPC 对象
     * @param profile 用户档案
     * @return 提示词字符串
     */
    public String buildNPCEvolutionPrompt(NPC npc, UserProfile profile) {
        return buildPrompt(
            "你是一个社会关系模拟器。",
            "玩家当前处境：" + profile.getCurrentScenario(),
            String.format("NPC资料：姓名=%s, 关系=%s, 年龄=%d, 状态=%s", npc.getName(), npc.getRelation(), npc.getAge(), npc.getStatus()),
            "任务：推演该NPC本年度的生活变故或与玩家的互动。",
            GUARDRAIL_CHINESE_ONLY + "\n限制：20字以内。"
        );
    }
    
    /**
     * 构建命运判定提示词
     * @param profile 用户档案
     * @param action 玩家行动
     * @param macroEvent 宏观事件
     * @param roll 随机值
     * @return 提示词字符串
     */
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

    /**
     * 构建通用提示词
     * @param layer1 角色层
     * @param layer2 世界层
     * @param layer3 用户层
     * @param layer4 任务层
     * @param layer5 约束层
     * @return 组合后的提示词
     */
    private String buildPrompt(String layer1, String layer2, String layer3, String layer4, String layer5) {
        StringBuilder sb = new StringBuilder();
        if (!layer1.isEmpty()) sb.append("### Role ###\n").append(layer1).append("\n\n");
        if (!layer2.isEmpty()) sb.append("### World Context ###\n").append(layer2).append("\n\n");
        if (!layer3.isEmpty()) sb.append("### User Profile ###\n").append(layer3).append("\n\n");
        sb.append("### Task ###\n").append(layer4).append("\n\n");
        sb.append("### Constraints ###\n").append(layer5);
        return sb.toString();
    }

    /**
     * 格式化用户上下文信息
     * @param profile 用户档案
     * @return 格式化后的字符串
     */
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
            "- 核心价值观：%s\n" +
            "- 长期记忆（重要）：%s",
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
            profile.getCoreValues() != null ? profile.getCoreValues().toString() : "[]",
            profile.getLongTermMemory() != null ? profile.getLongTermMemory() : "暂无"
        );
    }
    
    /**
     * 构建记忆压缩提示词
     * @param profile 用户档案
     * @param yearlyEvent 年度事件
     * @return 提示词字符串
     */
    public String buildMemoryConsolidationPrompt(UserProfile profile, String yearlyEvent) {
        return buildPrompt(
            SYSTEM_PERSONA_NARRATOR,
            "当前年份：" + profile.getCurrentAge() + "岁",
            "【当前记忆】" + (profile.getLongTermMemory() != null ? profile.getLongTermMemory() : "暂无") + "\n\n【本年新增经历】" + yearlyEvent,
            "任务：将【本年新增经历】压缩并合并到【当前记忆】中。\n" +
            "要求：\n" +
            "1. 仅保留对未来有深远影响的关键转折（如结婚、失业、亲人离世、获得大奖）。\n" +
            "2. 删除流水账和琐碎细节。\n" +
            "3. 保持时间线的连贯性。\n" +
            "4. 总字数控制在 500 字以内，像撰写人物小传一样。",
            GUARDRAIL_CHINESE_ONLY
        );
    }
}
