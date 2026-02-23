package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 代表系统的注册用户，主要用于身份识别和关联数据
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 手机号（唯一标识） */
    @Column(unique = true, nullable = false)
    private String phone;

    /** 注册时间 */
    private LocalDateTime createTime;

    public User() {
        this.createTime = LocalDateTime.now();
    }

    public User(String phone) {
        this.phone = phone;
        this.createTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
