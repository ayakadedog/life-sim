package com.example.demo.service;

import com.example.demo.model.Checkpoint;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.CheckpointRepository;
import com.example.demo.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersistenceService {

    @Autowired
    private CheckpointRepository checkpointRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void createCheckpoint(UserProfile profile) {
        try {
            Checkpoint cp = new Checkpoint();
            cp.setProfileId(profile.getId());
            cp.setAge(profile.getCurrentAge());
            // Serialize full object graph to JSON
            String json = objectMapper.writeValueAsString(profile);
            cp.setProfileJson(json);
            checkpointRepository.save(cp);
        } catch (Exception e) {
            e.printStackTrace(); // In prod, use logger
        }
    }

    public UserProfile rollback(String profileId, int targetAge) {
        List<Checkpoint> checkpoints = checkpointRepository.findByProfileIdOrderByAgeDesc(profileId);
        for (Checkpoint cp : checkpoints) {
            if (cp.getAge() == targetAge) {
                try {
                    UserProfile restored = objectMapper.readValue(cp.getProfileJson(), UserProfile.class);
                    // Overwrite current state in DB
                    return userProfileRepository.save(restored);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to restore checkpoint", e);
                }
            }
        }
        throw new RuntimeException("Checkpoint not found for age " + targetAge);
    }
    
    public List<Checkpoint> getHistory(String profileId) {
        return checkpointRepository.findByProfileIdOrderByAgeDesc(profileId);
    }
}
