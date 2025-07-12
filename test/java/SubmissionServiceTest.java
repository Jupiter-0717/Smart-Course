package com.example.smartlearn.service.stuednt;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.smartlearn.dto.request.SubmissionRequest;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Student;
import com.example.smartlearn.model.Submission;
import com.example.smartlearn.model.Task;
import com.example.smartlearn.repository.StudentRepository;
import com.example.smartlearn.repository.SubmissionRepository;
import com.example.smartlearn.repository.TaskRepository;
import com.example.smartlearn.service.student.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SubmissionService submissionService;

    @TempDir
    Path tempDir;

    private final Long testTaskId = 1L;
    private final Long testStudentId = 1L;
    private Task testTask;
    private Student testStudent;
    private SubmissionRequest testRequest;

    @BeforeEach
    void setUp() {
        // 设置上传目录为临时目录
        ReflectionTestUtils.setField(submissionService, "uploadDir", tempDir.toString());

        // 创建测试任务
        testTask = new Task();
        testTask.setId(testTaskId);
        testTask.setTitle("Test Task");
        testTask.setType(Task.TaskType.HOMEWORK);
        testTask.setDueDate(LocalDateTime.now().plusDays(1)); // 未过期

        // 创建测试学生
        testStudent = new Student();
        testStudent.setStudentId(testStudentId);
        testStudent.setName("Test Student");

        // 创建测试请求
        testRequest = new SubmissionRequest();
        testRequest.setTaskId(testTaskId);
        testRequest.setStudentId(testStudentId);
    }

    @Test
    void submitTask_Homework_Success() throws IOException {
        // 准备测试数据
        testTask.setType(Task.TaskType.HOMEWORK);
        testRequest.setContent("Homework content");

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.existsByTaskIdAndStudentStudentId(testTaskId, testStudentId)).thenReturn(false);

        // 执行测试
        submissionService.submitTask(testRequest);

        // 验证结果
        verify(submissionRepository).save(any(Submission.class));
    }


    @Test
    void submitTask_TaskNotFound() {
        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("任务不存在", exception.getMessage());
    }

    @Test
    void submitTask_StudentNotFound() {
        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("学生不存在", exception.getMessage());
    }

    @Test
    void submitTask_TaskExpired() {
        // 准备测试数据
        testTask.setDueDate(LocalDateTime.now().minusDays(1)); // 已过期

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("任务已过期，无法提交", exception.getMessage());
    }

    @Test
    void submitTask_AlreadySubmitted() {
        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.existsByTaskIdAndStudentStudentId(testTaskId, testStudentId)).thenReturn(true);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("已提交过该任务", exception.getMessage());
    }

    @Test
    void submitTask_HomeworkWithFiles() {
        // 准备测试数据
        testTask.setType(Task.TaskType.HOMEWORK);

        // 创建模拟文件
        MultipartFile mockFile = mock(MultipartFile.class);
        List<MultipartFile> files = Collections.singletonList(mockFile);
        testRequest.setFiles(files);

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.existsByTaskIdAndStudentStudentId(testTaskId, testStudentId)).thenReturn(false);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("作业类型任务只能提交文本内容", exception.getMessage());
    }

    @Test
    void submitTask_ReportWithContent() {
        // 准备测试数据
        testTask.setType(Task.TaskType.REPORT);
        testRequest.setContent("Report content");

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.existsByTaskIdAndStudentStudentId(testTaskId, testStudentId)).thenReturn(false);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("报告类型任务只能提交文件", exception.getMessage());
    }

    @Test
    void submitTask_ReportNoFiles() {
        // 准备测试数据
        testTask.setType(Task.TaskType.REPORT);

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.existsByTaskIdAndStudentStudentId(testTaskId, testStudentId)).thenReturn(false);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("请上传报告文件", exception.getMessage());
    }



    @Test
    void submitTask_HomeworkEmptyContent() {
        // 准备测试数据
        testTask.setType(Task.TaskType.HOMEWORK);
        testRequest.setContent(""); // 空内容

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.existsByTaskIdAndStudentStudentId(testTaskId, testStudentId)).thenReturn(false);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            submissionService.submitTask(testRequest);
        });

        assertEquals("作业内容不能为空", exception.getMessage());
    }


}