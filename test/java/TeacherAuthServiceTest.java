package com.example.smartlearn;

import com.example.smartlearn.model.Teacher;
import com.example.smartlearn.repository.TeacherRepository;
import com.example.smartlearn.service.auth.TeacherAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAuthServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TeacherAuthService service;

    @Test
    void getCurrentTeacherId_ValidUser_ReturnsId() {
        // 设置安全上下文
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("teacher1");

        // 模拟数据库响应
        Teacher teacher = new Teacher();
        teacher.setTeacherId(1001L);
        when(teacherRepository.findByName("teacher1")).thenReturn(Optional.of(teacher));

        // 执行测试
        Long result = service.getCurrentTeacherId(request);

        // 验证结果
        assertEquals(1001L, result);
    }

    @Test
    void getCurrentTeacherId_UserNotFound_ThrowsException() {
        // 设置安全上下文
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("unknown");

        // 模拟数据库响应
        when(teacherRepository.findByName("unknown")).thenReturn(Optional.empty());

        // 执行并验证异常
        Exception exception = assertThrows(RuntimeException.class,
                () -> service.getCurrentTeacherId(request));

        assertEquals("教师信息未找到", exception.getMessage());
    }

    @Test
    void getCurrentTeacherId_EmptyUsername_ThrowsException() {
        // 设置安全上下文
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("");

        // 执行并验证异常
        Exception exception = assertThrows(RuntimeException.class,
                () -> service.getCurrentTeacherId(request));

        assertEquals("教师信息未找到", exception.getMessage());
    }
}