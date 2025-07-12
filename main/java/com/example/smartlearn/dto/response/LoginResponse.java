package com.example.smartlearn.dto.response;
import com.example.smartlearn.model.Teacher;
import com.example.smartlearn.model.User;
import com.example.smartlearn.repository.TeacherRepository;
import com.example.smartlearn.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;


@Data
public class LoginResponse {
    private Long userId;
    private String account;
    private String role;
    private Long teacherId;
    private Long studentId;
    private String token;


    public LoginResponse(User user) {
        this.userId = user.getUserId();
        this.account = user.getAccount();
        this.role = user.getRole();

    }

    public LoginResponse(User user, Long teacherId, Long studentId) {
        this.userId = user.getUserId();
        this.account = user.getAccount();
        this.role = user.getRole();
        this.teacherId = teacherId;
        this.studentId = studentId;
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