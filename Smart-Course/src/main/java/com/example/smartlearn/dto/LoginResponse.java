package com.example.smartlearn.dto;

import com.example.smartlearn.model.User;

public class LoginResponse {
    private Long userId;
    private String account;
    private String role;

    public LoginResponse(User user) {
        this.userId = user.getUserId();
        this.account = user.getAccount();
        this.role = user.getRole();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}