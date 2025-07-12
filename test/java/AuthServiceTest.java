package com.example.smartlearn;

import com.example.smartlearn.dto.request.LoginRequest;
import com.example.smartlearn.dto.request.UserRegistrationRequest;
import com.example.smartlearn.dto.response.LoginResponse;
import com.example.smartlearn.exception.AccountAlreadyExistsException;
import com.example.smartlearn.exception.InvalidRoleException;
import com.example.smartlearn.model.Student;
import com.example.smartlearn.model.Teacher;
import com.example.smartlearn.model.User;
import com.example.smartlearn.repository.StudentRepository;
import com.example.smartlearn.repository.TeacherRepository;
import com.example.smartlearn.repository.UserRepository;
import com.example.smartlearn.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private AuthService authService;

    // 辅助方法：创建带参数的LoginRequest
    private LoginRequest createLoginRequest(String account, String password, String role) {
        LoginRequest request = new LoginRequest();
        request.setAccount(account);
        request.setPassword(password);
        request.setRole(role);
        return request;
    }

    // ============ authenticate 方法测试 ============

    @Test
    void authenticate_UserNotFound() {
        // 准备请求
        LoginRequest request = createLoginRequest("testUser", "password", "student");

        // 模拟用户不存在
        when(userRepository.findByAccountAndRole("testUser", "student"))
                .thenReturn(Optional.empty());

        // 执行并断言
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("账号不存在", exception.getReason());
    }

    @Test
    void authenticate_WrongPassword() {
        // 准备请求
        LoginRequest request = createLoginRequest("testUser", "wrongPassword", "teacher");

        // 模拟用户存在
        User user = new User();
        user.setAccount("testUser");
        user.setPasswordHash("correctPassword");
        user.setRole("teacher");

        when(userRepository.findByAccountAndRole("testUser", "teacher"))
                .thenReturn(Optional.of(user));

        // 执行并断言
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("密码错误", exception.getReason());
    }

    @Test
    void authenticate_TeacherSuccess() {
        // 准备请求
        LoginRequest request = createLoginRequest("teacherUser", "password", "teacher");

        // 模拟用户
        User user = new User();
        user.setAccount("teacherUser");
        user.setPasswordHash("password");
        user.setRole("teacher");

        // 模拟教师
        Teacher teacher = new Teacher();
        teacher.setTeacherId(1L);

        when(userRepository.findByAccountAndRole("teacherUser", "teacher"))
                .thenReturn(Optional.of(user));
        when(teacherRepository.findByUser(user))
                .thenReturn(Optional.of(teacher));

        // 执行
        LoginResponse response = authService.authenticate(request);

        // 断言
        assertNotNull(response);
        assertEquals("teacherUser", response.getAccount());
        assertEquals(1L, response.getTeacherId());
        assertNull(response.getStudentId());
    }

    @Test
    void authenticate_StudentSuccess() {
        // 准备请求
        LoginRequest request = createLoginRequest("studentUser", "password", "student");

        // 模拟用户
        User user = new User();
        user.setAccount("studentUser");
        user.setPasswordHash("password");
        user.setRole("student");

        // 模拟学生
        Student student = new Student();
        student.setStudentId(2L);

        when(userRepository.findByAccountAndRole("studentUser", "student"))
                .thenReturn(Optional.of(user));
        when(studentRepository.findByUser(user))
                .thenReturn(Optional.of(student));

        // 执行
        LoginResponse response = authService.authenticate(request);

        // 断言
        assertNotNull(response);
        assertEquals("studentUser", response.getAccount());
        assertEquals(2L, response.getStudentId());
        assertNull(response.getTeacherId());
    }

    @Test
    void authenticate_TeacherNotFound() {
        // 准备请求
        LoginRequest request = createLoginRequest("teacherUser", "password", "teacher");

        // 模拟用户
        User user = new User();
        user.setAccount("teacherUser");
        user.setPasswordHash("password");
        user.setRole("teacher");

        when(userRepository.findByAccountAndRole("teacherUser", "teacher"))
                .thenReturn(Optional.of(user));
        when(teacherRepository.findByUser(user))
                .thenReturn(Optional.empty());

        // 执行并断言
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(request));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("教师信息未找到", exception.getReason());
    }

    @Test
    void authenticate_StudentNotFound() {
        // 准备请求
        LoginRequest request = createLoginRequest("studentUser", "password", "student");

        // 模拟用户
        User user = new User();
        user.setAccount("studentUser");
        user.setPasswordHash("password");
        user.setRole("student");

        when(userRepository.findByAccountAndRole("studentUser", "student"))
                .thenReturn(Optional.of(user));
        when(studentRepository.findByUser(user))
                .thenReturn(Optional.empty());

        // 执行并断言
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(request));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertEquals("学生信息未找到", exception.getReason());
    }

    // ============ register 方法测试 ============

    @Test
    void register_AccountAlreadyExists() {
        // 准备请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setAccount("existingUser");
        request.setPassword("password");
        request.setRole("student");

        // 模拟账号已存在
        when(userRepository.existsByAccount("existingUser")).thenReturn(true);

        // 执行并断言
        AccountAlreadyExistsException exception = assertThrows(AccountAlreadyExistsException.class,
                () -> authService.register(request));

        assertEquals("该账号已存在", exception.getMessage());

        // 验证未调用保存方法
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_StudentSuccess() {
        // 准备请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setAccount("newStudent");
        request.setPassword("password");
        request.setRole("student");
        request.setName("John Doe");
        request.setClassName("Class A");
        request.setStudentId(123L);

        // 模拟账号不存在
        when(userRepository.existsByAccount("newStudent")).thenReturn(false);

        // 模拟保存用户
        User savedUser = new User();
        savedUser.setUserId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 执行
        authService.register(request);

        // 验证保存调用
        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any(Student.class));
        verify(teacherRepository, never()).save(any(Teacher.class));
    }

    @Test
    void register_TeacherSuccess() {
        // 准备请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setAccount("newTeacher");
        request.setPassword("password");
        request.setRole("teacher");
        request.setName("Jane Smith");
        request.setDepartment("Mathematics");

        // 模拟账号不存在
        when(userRepository.existsByAccount("newTeacher")).thenReturn(false);

        // 模拟保存用户
        User savedUser = new User();
        savedUser.setUserId(2L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 执行
        authService.register(request);

        // 验证保存调用
        verify(userRepository).save(any(User.class));
        verify(teacherRepository).save(any(Teacher.class));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void register_InvalidRole() {
        // 准备请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setAccount("invalidUser");
        request.setPassword("password");
        request.setRole("admin"); // 无效角色

        // 模拟账号不存在
        when(userRepository.existsByAccount("invalidUser")).thenReturn(false);

        // 修复：确保保存用户返回非空对象
        User mockUser = new User();
        when(userRepository.save(any(User.class))).thenReturn(mockUser); // 返回模拟用户对象

        // 执行并断言
        InvalidRoleException exception = assertThrows(InvalidRoleException.class,
                () -> authService.register(request));

        assertEquals("无效的角色类型: admin", exception.getMessage());

        // 验证用户保存被调用，但教师/学生保存未调用
        verify(userRepository).save(any(User.class));
        verify(teacherRepository, never()).save(any(Teacher.class));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void register_CaseInsensitiveRole() {
        // 准备请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setAccount("mixedCase");
        request.setPassword("password");
        request.setRole("TeAcHeR"); // 混合大小写

        // 模拟账号不存在
        when(userRepository.existsByAccount("mixedCase")).thenReturn(false);

        // 模拟保存用户
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // 执行
        authService.register(request);

        // 验证保存调用
        verify(teacherRepository).save(any(Teacher.class));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void register_TeacherWithDefaultAvatar() {
        // 准备请求
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setAccount("teacherUser");
        request.setPassword("password");
        request.setRole("teacher");
        request.setName("Professor X");
        request.setDepartment("Physics");

        // 模拟账号不存在
        when(userRepository.existsByAccount("teacherUser")).thenReturn(false);

        // 模拟保存用户
        User savedUser = new User();
        savedUser.setUserId(3L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 执行
        authService.register(request);

        // 验证教师保存时的默认头像
        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherRepository).save(teacherCaptor.capture());

        Teacher savedTeacher = teacherCaptor.getValue();
        assertEquals("/default-avatar.png", savedTeacher.getAvatarUrl());
    }
}