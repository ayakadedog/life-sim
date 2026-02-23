package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User loginOrRegister(String phone) {
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isPresent()) {
            return userOpt.get();
        } else {
            User newUser = new User(phone);
            return userRepository.save(newUser);
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
