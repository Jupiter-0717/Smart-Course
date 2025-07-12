package com.example.smartlearn.service.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.contains;

import com.example.smartlearn.dto.response.TaskAiScoreResultResponse;
import com.example.smartlearn.model.Student;
import com.example.smartlearn.model.Submission;
import com.example.smartlearn.model.Task;
import com.example.smartlearn.repository.SubmissionRepository;
import com.example.smartlearn.repository.TaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@ExtendWith(MockitoExtension.class)
class ReportAiScoreServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ReportAiScoreService reportAiScoreService;

    private final Long testTaskId = 1L;
    private final Long testSubmissionId = 1L;
    private final Long testStudentId = 1L;
    private Task testTask;
    private Student testStudent;
    private Submission testSubmission;
    private Map<String, Object> testCriteria;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(reportAiScoreService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(reportAiScoreService, "baseUrl", "http://localhost/v1");
        ReflectionTestUtils.setField(reportAiScoreService, "uploadDir", "/tmp/uploads");

        // 创建测试任务
        testTask = new Task();
        testTask.setId(testTaskId);
        testTask.setTitle("Test Report Task");
        testTask.setDescription("Test task description");
        testTask.setType(Task.TaskType.REPORT);

        // 创建测试学生
        testStudent = new Student();
        testStudent.setStudentId(testStudentId);
        testStudent.setStudentName("Test Student");

        // 创建测试提交
        testSubmission = new Submission();
        testSubmission.setId(testSubmissionId);
        testSubmission.setTask(testTask);
        testSubmission.setStudent(testStudent);
        testSubmission.setSubmittedAt(LocalDateTime.now());
        testSubmission.setFilePath("[{\"index\":0,\"original\":\"test.pdf\",\"saved\":\"/tmp/uploads/test.pdf\"}]");

        // 创建测试评分标准
        testCriteria = new HashMap<>();
        testCriteria.put("c1", 0.3);
        testCriteria.put("c2", 0.2);
        testCriteria.put("c3", 0.2);
        testCriteria.put("c4", 0.1);
        testCriteria.put("c5", 0.1);
        testCriteria.put("c6", 0.1);
    }

    @Test
    void getTaskAiScoreResults_Success() throws Exception {
        // 准备测试数据
        testSubmission.setReportAiScore("[{\"index\":0,\"paperOrder\":1,\"paperGrade\":85,\"c1\":0.8,\"c2\":0.7}]");
        List<Submission> submissions = Arrays.asList(testSubmission);

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(submissionRepository.findByTaskId(testTaskId)).thenReturn(submissions);
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Arrays.asList(createMockAiScoreMap()));

        // 执行测试
        TaskAiScoreResultResponse result = reportAiScoreService.getTaskAiScoreResults(testTaskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(testTaskId, result.getTaskId());
        assertEquals("Test Report Task", result.getTaskTitle());
        assertEquals(1, result.getStudentResults().size());
        
        TaskAiScoreResultResponse.StudentAiScoreResult studentResult = result.getStudentResults().get(0);
        assertEquals(testStudentId, studentResult.getStudentId());
        assertEquals("Test Student", studentResult.getStudentName());
        assertTrue(studentResult.getHasAiScore());
        assertEquals(1, studentResult.getFileScores().size());
        
        // 验证Mock被调用
        verify(taskRepository).findById(testTaskId);
        verify(submissionRepository).findByTaskId(testTaskId);
        verify(objectMapper).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    void getTaskAiScoreResults_TaskNotFound() {
        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reportAiScoreService.getTaskAiScoreResults(testTaskId);
        });

        assertEquals("任务不存在", exception.getMessage());
        
        // 验证Mock被调用
        verify(taskRepository).findById(testTaskId);
    }

    @Test
    void getTaskAiScoreResults_NoAiScore() throws Exception {
        // 准备测试数据 - 没有AI评分结果
        testSubmission.setReportAiScore(null);
        List<Submission> submissions = Arrays.asList(testSubmission);

        // 模拟依赖行为
        when(taskRepository.findById(testTaskId)).thenReturn(Optional.of(testTask));
        when(submissionRepository.findByTaskId(testTaskId)).thenReturn(submissions);

        // 执行测试
        TaskAiScoreResultResponse result = reportAiScoreService.getTaskAiScoreResults(testTaskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getStudentResults().size());
        assertFalse(result.getStudentResults().get(0).getHasAiScore());
        assertEquals(0, result.getStudentResults().get(0).getFileScores().size());
        
        // 验证Mock被调用
        verify(taskRepository).findById(testTaskId);
        verify(submissionRepository).findByTaskId(testTaskId);
    }


    @Test
    void aiScoreTask_InvalidCriteria() {
        // 准备无效的评分标准
        Map<String, Object> invalidCriteria = new HashMap<>();
        invalidCriteria.put("c1", 0.5);
        invalidCriteria.put("c2", 0.3);
        // 缺少其他权重，总和不为1

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reportAiScoreService.aiScoreTask(testTaskId, invalidCriteria);
        });

        assertTrue(exception.getMessage().contains("缺少权重参数") || 
                  exception.getMessage().contains("权重之和必须等于1"));
    }

    @Test
    void aiScoreTask_SubmissionWithoutFiles() throws Exception {
        // 准备测试数据 - 提交没有文件
        testSubmission.setFilePath(null);
        List<Submission> submissions = Arrays.asList(testSubmission);

        // 模拟依赖行为
        when(submissionRepository.findByTaskId(testTaskId)).thenReturn(submissions);

        // 执行测试
        reportAiScoreService.aiScoreTask(testTaskId, testCriteria);

        // 验证Mock被调用
        verify(submissionRepository).findByTaskId(testTaskId);
        // 验证没有调用保存方法
        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    void aiScoreSubmission_SubmissionNotFound() {
        // 准备测试数据
        List<Map<String, Object>> criteriaList = Arrays.asList(testCriteria);

        // 模拟依赖行为
        when(submissionRepository.findById(testSubmissionId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reportAiScoreService.aiScoreSubmission(testSubmissionId, criteriaList);
        });

        assertEquals("提交记录不存在", exception.getMessage());
        
        // 验证Mock被调用
        verify(submissionRepository).findById(testSubmissionId);
    }

    @Test
    void aiScoreSubmission_NoFiles() {
        // 准备测试数据 - 没有文件
        testSubmission.setFilePath(null);
        List<Map<String, Object>> criteriaList = Arrays.asList(testCriteria);

        // 模拟依赖行为
        when(submissionRepository.findById(testSubmissionId)).thenReturn(Optional.of(testSubmission));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reportAiScoreService.aiScoreSubmission(testSubmissionId, criteriaList);
        });

        assertEquals("无文件可批改", exception.getMessage());
        
        // 验证Mock被调用
        verify(submissionRepository).findById(testSubmissionId);
    }

    // 辅助方法：创建模拟的AI评分Map
    private Map<String, Object> createMockAiScoreMap() {
        Map<String, Object> aiScore = new HashMap<>();
        aiScore.put("index", 0);
        aiScore.put("paperOrder", 1);
        aiScore.put("paperGrade", 85);
        aiScore.put("c1", 0.8);
        aiScore.put("c2", 0.7);
        return aiScore;
    }

    // 辅助方法：创建模拟的Dify API响应
    private Map<String, Object> createMockDifyResponse() {
        Map<String, Object> structuredOutput = new HashMap<>();
        structuredOutput.put("paperOrder", 1);
        structuredOutput.put("paperGrade", 85);
        structuredOutput.put("c1", 0.8);
        structuredOutput.put("c2", 0.7);

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("structured_output", structuredOutput);

        Map<String, Object> data = new HashMap<>();
        data.put("outputs", outputs);

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        return response;
    }
} 