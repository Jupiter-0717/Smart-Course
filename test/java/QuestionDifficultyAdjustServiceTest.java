package com.example.smartlearn;

import com.example.smartlearn.dto.request.QuestionDifficultyAdjustRequest;
import com.example.smartlearn.dto.response.QuestionDifficultyAdjustResponse;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.KnowledgePoint;
import com.example.smartlearn.model.Question;
import com.example.smartlearn.model.StudentAnswer;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.repository.KnowledgePointRepository;
import com.example.smartlearn.repository.QuestionRepository;
import com.example.smartlearn.repository.StudentAnswerRepository;
import com.example.smartlearn.service.teacher.QuestionDifficultyAdjustService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionDifficultyAdjustServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private KnowledgePointRepository knowledgePointRepository;

    @InjectMocks
    private QuestionDifficultyAdjustService service;

    private final Logger logger = LoggerFactory.getLogger(QuestionDifficultyAdjustServiceTest.class);

    private final Long courseId = 101L;
    private final Long questionId = 201L;
    private final Long knowledgePointId = 301L;
    private Course course;
    private KnowledgePoint knowledgePoint;
    private Question question;
    private StudentAnswer studentAnswer;

    @BeforeEach
    void setUp() {
        // 创建课程
        course = new Course();
        course.setCourseId(courseId);
        course.setName("计算机科学");

        // 创建知识点
        knowledgePoint = new KnowledgePoint();
        knowledgePoint.setId(knowledgePointId);
        knowledgePoint.setName("面向对象编程");
        knowledgePoint.setCourse(course);

        // 创建题目
        question = new Question();
        question.setId(questionId);
        question.setCourse(course);
        question.setKnowledgePoint(knowledgePoint);
        question.setType(Question.QuestionType.SINGLE_CHOICE);
        question.setBody("Java中如何定义类？");
        question.setDifficulty(3);

        // 创建学生答案
        studentAnswer = new StudentAnswer();
        studentAnswer.setId(1L);
        studentAnswer.setQuestion(question);
        studentAnswer.setCorrect(true);
    }

    @Test
    void queryAdjustableQuestions_IncludeAllCourses_Success() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Query request = new QuestionDifficultyAdjustRequest.Query();
        request.setIncludeAllCourses(true);

        List<Question> questions = Collections.singletonList(question);
        List<StudentAnswer> answers = Collections.singletonList(studentAnswer);

        // 模拟依赖行为
        when(questionRepository.findAll()).thenReturn(questions);
        when(studentAnswerRepository.findByQuestionId(questionId)).thenReturn(answers);

        // 执行测试
        QuestionDifficultyAdjustResponse.QueryResult result = service.queryAdjustableQuestions(request);

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getQuestions());
        assertEquals(1, result.getQuestions().size());

        QuestionDifficultyAdjustResponse.QueryResult.QuestionStat stat = result.getQuestions().get(0);
        assertEquals(questionId, stat.getId());
        assertEquals("Java中如何定义类？", stat.getBody());
        assertEquals(3, stat.getCurrentDifficulty());
        assertEquals(1.0, stat.getCorrectRate(), 0.001);
        assertEquals(1, stat.getTotalAttempts());
        assertEquals(0, stat.getWrongAttempts());
        assertEquals("计算机科学", stat.getCourseName());
        assertEquals("面向对象编程", stat.getKnowledgePointName());
        assertEquals("正确率高于当前难度区间，建议提升难度", stat.getChangeReason());

        // 验证统计信息
        QuestionDifficultyAdjustResponse.QueryResult.Statistics statistics = result.getStatistics();
        assertNotNull(statistics);
        assertEquals(1, statistics.getTotalQuestions());
        assertEquals(1, statistics.getNeedAdjustment());
        assertEquals(1.0, statistics.getAverageCorrectRate(), 0.001);
    }

    @Test
    void queryAdjustableQuestions_ByCourseIds_Success() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Query request = new QuestionDifficultyAdjustRequest.Query();
        request.setCourseIds(Collections.singletonList(courseId));

        List<Question> questions = Collections.singletonList(question);
        List<StudentAnswer> answers = Collections.singletonList(studentAnswer);

        // 模拟依赖行为
        when(questionRepository.findByCourseCourseId(courseId)).thenReturn(questions);
        when(studentAnswerRepository.findByQuestionId(questionId)).thenReturn(answers);

        // 执行测试
        QuestionDifficultyAdjustResponse.QueryResult result = service.queryAdjustableQuestions(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getQuestions().size());
    }

    @Test
    void queryAdjustableQuestions_NotEnoughAttempts() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Query request = new QuestionDifficultyAdjustRequest.Query();
        request.setIncludeAllCourses(true);

        List<Question> questions = Collections.singletonList(question);
        List<StudentAnswer> answers = Arrays.asList(studentAnswer, studentAnswer); // 只有2次尝试

        // 模拟依赖行为
        when(questionRepository.findAll()).thenReturn(questions);
        when(studentAnswerRepository.findByQuestionId(questionId)).thenReturn(answers);

        // 执行测试
        QuestionDifficultyAdjustResponse.QueryResult result = service.queryAdjustableQuestions(request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getQuestions().isEmpty()); // 尝试次数不足10次，不包含在结果中
    }

    @Test
    void queryAdjustableQuestions_NoAnswers() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Query request = new QuestionDifficultyAdjustRequest.Query();
        request.setIncludeAllCourses(true);

        List<Question> questions = Collections.singletonList(question);

        // 模拟依赖行为
        when(questionRepository.findAll()).thenReturn(questions);
        when(studentAnswerRepository.findByQuestionId(questionId)).thenReturn(Collections.emptyList());

        // 执行测试
        QuestionDifficultyAdjustResponse.QueryResult result = service.queryAdjustableQuestions(request);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.getQuestions().isEmpty()); // 没有答题记录，不包含在结果中
    }

    @Test
    void queryAdjustableQuestions_ExceptionHandling() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Query request = new QuestionDifficultyAdjustRequest.Query();
        request.setIncludeAllCourses(true);

        // 模拟依赖行为抛出异常
        when(questionRepository.findAll()).thenThrow(new RuntimeException("数据库连接失败"));

        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.queryAdjustableQuestions(request);
        });

        assertEquals("题目难度动态调整-曹雨荷部分：查询可调整题目时发生异常: 数据库连接失败", exception.getMessage());
    }

    @Test
    void confirmAdjustments_Success() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Confirm request = new QuestionDifficultyAdjustRequest.Confirm();

        QuestionDifficultyAdjustRequest.Confirm.Adjustment adjustment =
                new QuestionDifficultyAdjustRequest.Confirm.Adjustment();
        adjustment.setQuestionId(questionId);
        adjustment.setShouldAdjust(true);
        adjustment.setNewDifficulty(4);

        request.setAdjustments(Collections.singletonList(adjustment));

        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        // 执行测试
        QuestionDifficultyAdjustResponse.ConfirmResult result = service.confirmAdjustments(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getDetails().size());

        QuestionDifficultyAdjustResponse.ConfirmResult.Detail detail = result.getDetails().get(0);
        assertEquals(questionId, detail.getQuestionId());
        assertEquals(3, detail.getOldDifficulty());
        assertEquals(4, detail.getNewDifficulty());
        assertEquals("success", detail.getStatus());

        // 验证题目难度已更新
        assertEquals(4, question.getDifficulty());
    }

    @Test
    void confirmAdjustments_QuestionNotFound() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Confirm request = new QuestionDifficultyAdjustRequest.Confirm();

        QuestionDifficultyAdjustRequest.Confirm.Adjustment adjustment =
                new QuestionDifficultyAdjustRequest.Confirm.Adjustment();
        adjustment.setQuestionId(999L); // 不存在的题目ID
        adjustment.setShouldAdjust(true);
        adjustment.setNewDifficulty(4);

        request.setAdjustments(Collections.singletonList(adjustment));

        // 模拟依赖行为
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试
        QuestionDifficultyAdjustResponse.ConfirmResult result = service.confirmAdjustments(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getDetails().size());

        QuestionDifficultyAdjustResponse.ConfirmResult.Detail detail = result.getDetails().get(0);
        assertEquals(999L, detail.getQuestionId());
        assertEquals("failed", detail.getStatus());
    }

    @Test
    void confirmAdjustments_Skipped() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Confirm request = new QuestionDifficultyAdjustRequest.Confirm();

        QuestionDifficultyAdjustRequest.Confirm.Adjustment adjustment =
                new QuestionDifficultyAdjustRequest.Confirm.Adjustment();
        adjustment.setQuestionId(questionId);
        adjustment.setShouldAdjust(false); // 跳过调整

        request.setAdjustments(Collections.singletonList(adjustment));

        // 执行测试
        QuestionDifficultyAdjustResponse.ConfirmResult result = service.confirmAdjustments(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getDetails().size());

        QuestionDifficultyAdjustResponse.ConfirmResult.Detail detail = result.getDetails().get(0);
        assertEquals(questionId, detail.getQuestionId());
        assertEquals("skipped", detail.getStatus());
    }

    @Test
    void confirmAdjustments_Exception() {
        // 准备测试数据
        QuestionDifficultyAdjustRequest.Confirm request = new QuestionDifficultyAdjustRequest.Confirm();

        QuestionDifficultyAdjustRequest.Confirm.Adjustment adjustment =
                new QuestionDifficultyAdjustRequest.Confirm.Adjustment();
        adjustment.setQuestionId(questionId);
        adjustment.setShouldAdjust(true);
        adjustment.setNewDifficulty(4);

        request.setAdjustments(Collections.singletonList(adjustment));

        // 模拟依赖行为抛出异常
        when(questionRepository.findById(questionId)).thenThrow(new RuntimeException("数据库错误"));

        // 执行测试
        QuestionDifficultyAdjustResponse.ConfirmResult result = service.confirmAdjustments(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getDetails().size());

        QuestionDifficultyAdjustResponse.ConfirmResult.Detail detail = result.getDetails().get(0);
        assertEquals(questionId, detail.getQuestionId());
        assertEquals("failed", detail.getStatus());
    }



}