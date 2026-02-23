package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户服务
 * 处理用户注册、登录和信息查询等业务逻辑
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 登录或注册
     * 如果手机号已存在则返回现有用户，否则创建新用户
     * @param phone 手机号
     * @return 登录或注册成功的用户对象
     */
    public User loginOrRegister(String phone) {
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isPresent()) {
            return userOpt.get();
        } else {
            User newUser = new User(phone);
            return userRepository.save(newUser);
        }
    }

    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户对象，如果不存在则返回 null
     */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
