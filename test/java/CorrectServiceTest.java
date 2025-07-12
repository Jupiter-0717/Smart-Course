package com.example.smartlearn.service.teacher;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.smartlearn.dto.response.*;
import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.CorrectRepository;
import com.example.smartlearn.repository.StudentRepository;
import com.example.smartlearn.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CorrectServiceTest {

    @Mock
    private CorrectRepository correctRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CorrectService correctService;

    private final Logger logger = LoggerFactory.getLogger(CorrectServiceTest.class);

    private final Long studentId = 1L;
    private final Long taskId = 101L;
    private final Long submissionId = 201L;
    private final Long questionId = 301L;
    private Student student;
    private Task task;
    private Submission submission;
    private Question question;
    private StudentAnswer studentAnswer;

    @BeforeEach
    void setUp() {
        // 设置上传目录
        ReflectionTestUtils.setField(correctService, "uploadDir", "/test/uploads");

        // 创建学生
        student = new Student();
        student.setStudentId(studentId);
        student.setStudentName("张三");

        // 创建任务
        task = new Task();
        task.setId(taskId);
        task.setTitle("数学作业");
        task.setDescription("完成课后习题");
        task.setType(Task.TaskType.HOMEWORK);

        // 创建问题
        question = new Question();
        question.setId(questionId);
        question.setBody("{\"stem\": \"1+1等于多少？\", \"options\": [\"1\", \"2\", \"3\"], \"answer\": \"2\"}");
        question.setType(Question.QuestionType.SINGLE_CHOICE);

        // 创建学生答案
        studentAnswer = new StudentAnswer();
        studentAnswer.setId(1L);
        studentAnswer.setAnswerContent("2");
        studentAnswer.setQuestion(question);

        // 创建提交
        submission = new Submission();
        submission.setId(submissionId);
        submission.setStudent(student);
        submission.setTask(task);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setContent("作业内容");
        submission.setFilePath("/uploads/assignment.pdf");
        submission.setStudentAnswers(Collections.singletonList(studentAnswer));
    }



    @Test
    void getFileSubmissionsByTask_Success() {
        // 模拟依赖行为
        when(correctRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(submission));

        // 执行测试
        List<CorrectResponse> result = correctService.getFileSubmissionsByTask(taskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        CorrectResponse response = result.get(0);
        assertEquals("assignment.pdf", response.getFileName());
    }

    @Test
    void getTextSubmissionsByTask_Success() {
        // 设置提交只有文本内容
        submission.setFilePath(null);

        // 模拟依赖行为
        when(correctRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(submission));

        // 执行测试
        List<CorrectResponse> result = correctService.getTextSubmissionsByTask(taskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        CorrectResponse response = result.get(0);
        assertEquals("TEXT", response.getSubmissionType());
    }

    @Test
    void getGradedSubmissionsByTask_Success() {
        // 设置已批改
        submission.setGrade(BigDecimal.valueOf(90));

        // 模拟依赖行为
        when(correctRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(submission));

        // 执行测试
        List<CorrectResponse> result = correctService.getGradedSubmissionsByTask(taskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(BigDecimal.valueOf(90), result.get(0).getGrade());
    }

    @Test
    void getUngradedSubmissionsByTask_Success() {
        // 设置未批改
        submission.setGrade(null);

        // 模拟依赖行为
        when(correctRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(submission));

        // 执行测试
        List<CorrectResponse> result = correctService.getUngradedSubmissionsByTask(taskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getGrade());
    }



    @Test
    void getSubmissionDetail_NotFound() {
        // 模拟依赖行为
        when(correctRepository.findByIdWithStudentAnswers(submissionId)).thenReturn(Optional.empty());

        // 执行测试
        Optional<CorrectDetailResponse> result = correctService.getSubmissionDetail(submissionId);

        // 验证结果
        assertFalse(result.isPresent());
    }

    @Test
    void correctSubmission_Success() {
        // 模拟依赖行为
        when(correctRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        // 执行测试
        boolean result = correctService.correctSubmission(submissionId, BigDecimal.valueOf(95), "优秀作业");

        // 验证结果
        assertTrue(result);
        assertEquals(BigDecimal.valueOf(95), submission.getGrade());
        assertEquals("优秀作业", submission.getFeedback());
        verify(correctRepository).save(submission);
    }

    @Test
    void correctSubmission_NotFound() {
        // 模拟依赖行为
        when(correctRepository.findById(submissionId)).thenReturn(Optional.empty());

        // 执行测试
        boolean result = correctService.correctSubmission(submissionId, BigDecimal.valueOf(95), "优秀作业");

        // 验证结果
        assertFalse(result);
    }

    @Test
    void getFileDownloadUrl_SimplePath() {
        // 准备测试数据
        String filePath = "/uploads/assignment.pdf";

        // 执行测试
        String result = correctService.getFileDownloadUrl(filePath);

        // 验证结果
        assertEquals("/uploads/assignment.pdf", result);
    }


    @Test
    void getFileDownloadUrl_FullUrl() {
        // 准备测试数据
        String url = "https://example.com/uploads/assignment.pdf";

        // 执行测试
        String result = correctService.getFileDownloadUrl(url);

        // 验证结果
        assertEquals(url, result);
    }

    @Test
    void getFileName_SimplePath() {
        // 准备测试数据
        String filePath = "/uploads/assignment.pdf";

        // 执行测试
        String result = correctService.getFileName(filePath);

        // 验证结果
        assertEquals("assignment.pdf", result);
    }





    @Test
    void getUngradedQuizShortAnswers_Success() {
        // 准备测试数据
        Task quizTask = new Task();
        quizTask.setId(taskId);
        quizTask.setType(Task.TaskType.QUIZ);

        Question shortAnswerQuestion = new Question();
        shortAnswerQuestion.setId(questionId);
        shortAnswerQuestion.setType(Question.QuestionType.SHORT_ANSWER);
        shortAnswerQuestion.setBody("解释牛顿第一定律");

        StudentAnswer ungradedAnswer = new StudentAnswer();
        ungradedAnswer.setId(1L);
        ungradedAnswer.setQuestion(shortAnswerQuestion);
        ungradedAnswer.setAnswerContent("物体在没有外力作用时保持静止或匀速直线运动");
        ungradedAnswer.setScore(null); // 未评分

        Submission quizSubmission = new Submission();
        quizSubmission.setId(submissionId);
        quizSubmission.setStudent(student);
        quizSubmission.setTask(quizTask);
        quizSubmission.setSubmittedAt(LocalDateTime.now());
        quizSubmission.setStudentAnswers(Collections.singletonList(ungradedAnswer));

        // 模拟依赖行为
        when(correctRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(quizSubmission));

        // 执行测试
        List<UngradedQuizAnswerResponse> result = correctService.getUngradedQuizShortAnswers(taskId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        UngradedQuizAnswerResponse response = result.get(0);
        assertEquals(submissionId, response.getSubmissionId());
        assertEquals(studentId, response.getStudentId());
        assertEquals("张三", response.getStudentName());
        assertEquals(questionId, response.getQuestionId());
        assertEquals("解释牛顿第一定律", response.getQuestionTitle());
        assertEquals("物体在没有外力作用时保持静止或匀速直线运动", response.getStudentAnswer());
    }

    @Test
    void getUngradedQuizShortAnswers_NoUngraded() {
        // 准备测试数据
        Task quizTask = new Task();
        quizTask.setId(taskId);
        quizTask.setType(Task.TaskType.QUIZ);

        Question shortAnswerQuestion = new Question();
        shortAnswerQuestion.setId(questionId);
        shortAnswerQuestion.setType(Question.QuestionType.SHORT_ANSWER);
        shortAnswerQuestion.setBody("解释牛顿第一定律");

        // 已评分的答案
        StudentAnswer gradedAnswer = new StudentAnswer();
        gradedAnswer.setId(1L);
        gradedAnswer.setQuestion(shortAnswerQuestion);
        gradedAnswer.setAnswerContent("物体在没有外力作用时保持静止或匀速直线运动");
        gradedAnswer.setScore(BigDecimal.valueOf(10)); // 已评分

        Submission quizSubmission = new Submission();
        quizSubmission.setId(submissionId);
        quizSubmission.setStudent(student);
        quizSubmission.setTask(quizTask);
        quizSubmission.setSubmittedAt(LocalDateTime.now());
        quizSubmission.setStudentAnswers(Collections.singletonList(gradedAnswer));

        // 模拟依赖行为
        when(correctRepository.findByTaskId(taskId)).thenReturn(Collections.singletonList(quizSubmission));

        // 执行测试
        List<UngradedQuizAnswerResponse> result = correctService.getUngradedQuizShortAnswers(taskId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void gradeQuizShortAnswer_Success() {
        // 准备测试数据
        Task quizTask = new Task();
        quizTask.setId(taskId);
        quizTask.setType(Task.TaskType.QUIZ);

        Question shortAnswerQuestion = new Question();
        shortAnswerQuestion.setId(questionId);
        shortAnswerQuestion.setType(Question.QuestionType.SHORT_ANSWER);

        StudentAnswer ungradedAnswer = new StudentAnswer();
        ungradedAnswer.setId(1L);
        ungradedAnswer.setQuestion(shortAnswerQuestion);
        ungradedAnswer.setAnswerContent("物体在没有外力作用时保持静止或匀速直线运动");
        ungradedAnswer.setScore(null); // 未评分

        Submission quizSubmission = new Submission();
        quizSubmission.setId(submissionId);
        quizSubmission.setStudent(student);
        quizSubmission.setTask(quizTask);
        quizSubmission.setSubmittedAt(LocalDateTime.now());
        quizSubmission.setStudentAnswers(Collections.singletonList(ungradedAnswer));

        // 模拟依赖行为
        when(correctRepository.findById(submissionId)).thenReturn(Optional.of(quizSubmission));

        // 执行测试
        boolean result = correctService.gradeQuizShortAnswer(submissionId, questionId, BigDecimal.valueOf(10), "回答正确");

        // 验证结果
        assertTrue(result);
        assertEquals(BigDecimal.valueOf(10), ungradedAnswer.getScore());
        assertEquals(BigDecimal.valueOf(10), quizSubmission.getGrade());
        verify(correctRepository).save(quizSubmission);
    }

    @Test
    void gradeQuizShortAnswer_NotFound() {
        // 模拟依赖行为
        when(correctRepository.findById(submissionId)).thenReturn(Optional.empty());

        // 执行测试
        boolean result = correctService.gradeQuizShortAnswer(submissionId, questionId, BigDecimal.valueOf(10), "回答正确");

        // 验证结果
        assertFalse(result);
    }

    @Test
    void gradeQuizShortAnswer_QuestionNotFound() {
        // 准备测试数据
        Task quizTask = new Task();
        quizTask.setId(taskId);
        quizTask.setType(Task.TaskType.QUIZ);

        Question otherQuestion = new Question();
        otherQuestion.setId(999L); // 不同的题目ID
        otherQuestion.setType(Question.QuestionType.SHORT_ANSWER);

        StudentAnswer ungradedAnswer = new StudentAnswer();
        ungradedAnswer.setId(1L);
        ungradedAnswer.setQuestion(otherQuestion);
        ungradedAnswer.setAnswerContent("其他答案");
        ungradedAnswer.setScore(null); // 未评分

        Submission quizSubmission = new Submission();
        quizSubmission.setId(submissionId);
        quizSubmission.setStudent(student);
        quizSubmission.setTask(quizTask);
        quizSubmission.setSubmittedAt(LocalDateTime.now());
        quizSubmission.setStudentAnswers(Collections.singletonList(ungradedAnswer));

        // 模拟依赖行为
        when(correctRepository.findById(submissionId)).thenReturn(Optional.of(quizSubmission));

        // 执行测试
        boolean result = correctService.gradeQuizShortAnswer(submissionId, questionId, BigDecimal.valueOf(10), "回答正确");

        // 验证结果
        assertFalse(result);
    }

    @Test
    void extractCorrectAnswer_InvalidJson() throws Exception {
        // 准备测试数据
        Question question = new Question();
        question.setId(questionId);
        question.setBody("Invalid JSON");
        question.setType(Question.QuestionType.SINGLE_CHOICE);

        // 使用反射调用私有方法
        Method method = CorrectService.class.getDeclaredMethod("extractCorrectAnswer", Question.class);
        method.setAccessible(true);
        String result = (String) method.invoke(correctService, question);

        // 验证结果
        assertEquals("解析失败", result);
    }

    @Test
    void extractCorrectAnswer_FillInBlank() throws Exception {
        // 准备测试数据
        Question question = new Question();
        question.setId(questionId);
        question.setBody("{\"stem\": \"水的化学式是____\", \"answer\": \"H₂O\"}");
        question.setType(Question.QuestionType.FILL_IN_BLANK);

        // 使用反射调用私有方法
        Method method = CorrectService.class.getDeclaredMethod("extractCorrectAnswer", Question.class);
        method.setAccessible(true);
        String result = (String) method.invoke(correctService, question);

        // 验证结果
        assertEquals("H₂O", result);
    }

    @Test
    void extractCorrectAnswer_MultiChoice() throws Exception {
        // 准备测试数据
        Question question = new Question();
        question.setId(questionId);
        question.setBody("{\"stem\": \"哪些是水果？\", \"options\": [\"苹果\", \"香蕉\", \"土豆\"], \"answer\": [\"苹果\", \"香蕉\"]}");
        question.setType(Question.QuestionType.MULTI_CHOICE);

        // 使用反射调用私有方法
        Method method = CorrectService.class.getDeclaredMethod("extractCorrectAnswer", Question.class);
        method.setAccessible(true);
        String result = (String) method.invoke(correctService, question);

        // 验证结果
        assertEquals("苹果,香蕉", result);
    }

    @Test
    void extractCorrectAnswer_SingleChoice() throws Exception {
        // 准备测试数据
        Question question = new Question();
        question.setId(questionId);
        question.setBody("{\"stem\": \"1+1等于多少？\", \"options\": [\"1\", \"2\", \"3\"], \"answer\": \"2\"}");
        question.setType(Question.QuestionType.SINGLE_CHOICE);

        // 使用反射调用私有方法
        Method method = CorrectService.class.getDeclaredMethod("extractCorrectAnswer", Question.class);
        method.setAccessible(true);
        String result = (String) method.invoke(correctService, question);

        // 验证结果
        assertEquals("2", result);
    }

    @Test
    void parseQuestionStem_InvalidJson() throws Exception {
        // 准备测试数据
        String questionBody = "Invalid JSON";

        // 使用反射调用私有方法
        Method method = CorrectService.class.getDeclaredMethod("parseQuestionStem", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(correctService, questionBody);

        // 验证结果
        assertEquals("Invalid JSON", result);
    }

    @Test
    void parseQuestionStem_Success() throws Exception {
        // 准备测试数据
        String questionBody = "{\"stem\": \"1+1等于多少？\", \"options\": [\"1\", \"2\", \"3\"], \"answer\": \"2\"}";

        // 使用反射调用私有方法
        Method method = CorrectService.class.getDeclaredMethod("parseQuestionStem", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(correctService, questionBody);

        // 验证结果
        assertEquals("1+1等于多少？", result);
    }


}