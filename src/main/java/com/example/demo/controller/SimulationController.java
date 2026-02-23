package com.example.demo.controller;

import com.example.demo.model.CharacterTemplate;
import com.example.demo.model.GameInstance;
import com.example.demo.model.User;
import com.example.demo.model.UserProfile;
import com.example.demo.service.SimulationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟人生控制器
 * 提供用户管理、模板管理、游戏实例管理以及模拟人生核心流程的 API 接口
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;
    
    @Autowired
    private UserService userService;

    // --- User Management ---

    /**
     * 用户登录或注册
     * @param request 包含手机号的请求体
     * @return 登录或注册后的用户信息
     */
    @PostMapping("/user/login")
    public User login(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        return userService.loginOrRegister(phone);
    }
    
    /**
     * 获取用户的角色模板列表
     * @param id 用户ID
     * @return 角色模板列表
     */
    @GetMapping("/user/{id}/templates")
    public List<CharacterTemplate> getUserTemplates(@PathVariable Long id) {
        return simulationService.getUserTemplates(id);
    }
    
    /**
     * 获取用户的游戏历史记录
     * @param id 用户ID
     * @return 游戏实例列表
     */
    @GetMapping("/user/{id}/history")
    public List<GameInstance> getUserHistory(@PathVariable Long id) {
        return simulationService.getUserHistory(id);
    }

    // --- Template & Game Creation ---

    /**
     * 创建新的角色模板
     * @param userId 用户ID
     * @param profile 用户档案信息
     * @return 创建的角色模板
     */
    @PostMapping("/template/create")
    public CharacterTemplate createTemplate(@RequestParam Long userId, @RequestBody UserProfile profile) {
        return simulationService.createTemplate(userId, profile);
    }
    
    /**
     * 开始新游戏
     * @param userId 用户ID
     * @param templateId 模板ID（可选，如果为null则视为新档案）
     * @param profile 用户档案信息（如果是从模板创建，此参数可能包含覆盖信息或被忽略，具体取决于实现）
     * @return 创建的游戏实例
     */
    @PostMapping("/game/start")
    public GameInstance startGame(@RequestParam Long userId, @RequestParam(required = false) Long templateId, @RequestBody UserProfile profile) {
        return simulationService.startGame(userId, templateId, profile);
    }
    
    /**
     * 获取游戏实例详情
     * @param id 游戏实例ID
     * @return 游戏实例信息
     */
    @GetMapping("/game/{id}")
    public GameInstance getGameInstance(@PathVariable Long id) {
        return simulationService.getGameInstance(id);
    }

    // --- Simulation Flow (Delegated to existing logic but using Profile ID) ---
    
    /**
     * 初始化档案（临时）
     * 用于用户填写表单时获取档案ID，以便后续进行探测问题生成
     * @param profile 初始档案信息
     * @return 初始化后的档案信息
     */
    @PostMapping("/sim/init")
    public UserProfile initProfile(@RequestBody UserProfile profile) {
        return simulationService.createProfile(profile);
    }
    
    /**
     * 生成探测问题
     * 根据当前档案信息生成进一步了解角色的问题
     * @param profile 当前档案信息
     * @return 探测问题列表
     */
    @PostMapping("/sim/probes")
    public List<String> getProbes(@RequestBody UserProfile profile) {
        return simulationService.generateProbes(profile);
    }

    /**
     * 提交探测问题答案并开始模拟
     * 根据用户的回答完善档案，并正式开始模拟
     * @param id 档案ID
     * @param answers 问题的答案映射
     * @return 更新后的档案信息
     */
    @PostMapping("/sim/{id}/start")
    public UserProfile startSimulation(@PathVariable String id, @RequestBody Map<String, String> answers) {
        return simulationService.analyzeProbesAndStart(id, answers);
    }

    /**
     * 模拟下一年
     * 用户做出选择后，推进时间并更新状态
     * @param id 档案ID
     * @param request 包含用户选择的请求体
     * @return 更新后的档案信息
     */
    @PostMapping("/sim/{id}/next")
    public UserProfile nextYear(@PathVariable String id, @RequestBody Map<String, String> request) {
        String choice = request.getOrDefault("choice", "平稳度过");
        return simulationService.simulateYear(id, choice);
    }
    
    /**
     * 跳过多年
     * 快速推进时间，通常用于平稳度过一段时间
     * @param id 档案ID
     * @param years 跳过的年数
     * @return 更新后的档案信息
     */
    @PostMapping("/sim/{id}/skip")
    public UserProfile skipYears(@PathVariable String id, @RequestParam int years) {
        return simulationService.skipYears(id, years);
    }

    /**
     * 生成人生总结/遗产
     * 模拟结束时调用，生成总结报告
     * @param id 档案ID
     * @return 最终的档案信息（包含总结）
     */
    @PostMapping("/sim/{id}/legacy")
    public UserProfile createLegacy(@PathVariable String id) {
        return simulationService.createLegacy(id);
    }
}
