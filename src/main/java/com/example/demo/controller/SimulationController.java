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

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;
    
    @Autowired
    private UserService userService;

    // --- User Management ---
    @PostMapping("/user/login")
    public User login(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        return userService.loginOrRegister(phone);
    }
    
    @GetMapping("/user/{id}/templates")
    public List<CharacterTemplate> getUserTemplates(@PathVariable Long id) {
        return simulationService.getUserTemplates(id);
    }
    
    @GetMapping("/user/{id}/history")
    public List<GameInstance> getUserHistory(@PathVariable Long id) {
        return simulationService.getUserHistory(id);
    }

    // --- Template & Game Creation ---
    @PostMapping("/template/create")
    public CharacterTemplate createTemplate(@RequestParam Long userId, @RequestBody UserProfile profile) {
        return simulationService.createTemplate(userId, profile);
    }
    
    // Start a new game from a template (or fresh profile)
    // If templateId is null, it treats profile as a fresh one
    @PostMapping("/game/start")
    public GameInstance startGame(@RequestParam Long userId, @RequestParam(required = false) Long templateId, @RequestBody UserProfile profile) {
        return simulationService.startGame(userId, templateId, profile);
    }
    
    @GetMapping("/game/{id}")
    public GameInstance getGameInstance(@PathVariable Long id) {
        return simulationService.getGameInstance(id);
    }

    // --- Simulation Flow (Delegated to existing logic but using Profile ID) ---
    
    // 1. Init Profile (Transient, before saving to template/game)
    // This is used when user is filling the form to get an ID for probes
    @PostMapping("/sim/init")
    public UserProfile initProfile(@RequestBody UserProfile profile) {
        return simulationService.createProfile(profile);
    }
    
    // 2. Get Probes
    @PostMapping("/sim/probes")
    public List<String> getProbes(@RequestBody UserProfile profile) {
        return simulationService.generateProbes(profile);
    }

    // 3. Submit Answers & Start (Updates the Profile)
    // IMPORTANT: This should be called AFTER startGame if we want to persist it properly in a GameInstance context
    // OR we can just update the profile linked to the GameInstance.
    @PostMapping("/sim/{id}/start")
    public UserProfile startSimulation(@PathVariable String id, @RequestBody Map<String, String> answers) {
        return simulationService.analyzeProbesAndStart(id, answers);
    }

    @PostMapping("/sim/{id}/next")
    public UserProfile nextYear(@PathVariable String id, @RequestBody Map<String, String> request) {
        String choice = request.getOrDefault("choice", "平稳度过");
        return simulationService.simulateYear(id, choice);
    }
    
    @PostMapping("/sim/{id}/skip")
    public UserProfile skipYears(@PathVariable String id, @RequestParam int years) {
        return simulationService.skipYears(id, years);
    }

    @PostMapping("/sim/{id}/legacy")
    public UserProfile createLegacy(@PathVariable String id) {
        return simulationService.createLegacy(id);
    }
}
