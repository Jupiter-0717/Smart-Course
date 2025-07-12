package com.example.smartlearn;


import com.example.smartlearn.dto.request.QuestionFilterRequest;
import com.example.smartlearn.dto.request.QuestionRequest;
import com.example.smartlearn.dto.response.QuestionResponse;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.KnowledgePoint;
import com.example.smartlearn.model.Question;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.repository.KnowledgePointRepository;
import com.example.smartlearn.repository.QuestionRepository;
import com.example.smartlearn.service.teacher.QuestionBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private KnowledgePointRepository knowledgePointRepository;

    @InjectMocks
    private QuestionBankService questionBankService;

    private final Long teacherId = 1L;
    private final Long courseId = 101L;
    private final Long questionId = 201L;
    private final Long knowledgePointId = 301L;
    private Course course;
    private KnowledgePoint knowledgePoint;
    private Question question;
    private QuestionRequest questionRequest;

    @BeforeEach
    void setUp() {
        // 创建课程
        course = new Course();
        course.setCourseId(courseId);
        course.setTeacherId(teacherId);
        course.setName("计算机科学");

        // 创建知识点
        knowledgePoint = new KnowledgePoint();
        knowledgePoint.setId(knowledgePointId);
        knowledgePoint.setCourse(course);
        knowledgePoint.setName("面向对象编程");

        // 创建题目
        question = new Question();
        question.setId(questionId);
        question.setCourse(course);
        question.setKnowledgePoint(knowledgePoint);
        question.setType(Question.QuestionType.SINGLE_CHOICE);
        question.setBody("Java中如何定义类？");
        question.setDifficulty(3);

        // 创建题目请求
        questionRequest = new QuestionRequest();
        questionRequest.setCourseId(courseId);
        questionRequest.setKnowledgePointId(knowledgePointId);
        questionRequest.setType("SINGLE_CHOICE");
        questionRequest.setBody("Java中如何定义类？");
        questionRequest.setDifficulty(3);
    }

    @Test
    void createQuestion_Success() {
        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(knowledgePointRepository.findById(knowledgePointId)).thenReturn(Optional.of(knowledgePoint));
        when(questionRepository.save(any(Question.class))).thenReturn(question);

        // 执行测试
        QuestionResponse response = questionBankService.createQuestion(questionRequest, teacherId);

        // 验证结果
        assertNotNull(response);
        assertEquals(questionId, response.getId());
        assertEquals(courseId, response.getCourseId());
        assertEquals(knowledgePointId, response.getKnowledgePointId());
        assertEquals("SINGLE_CHOICE", response.getType());
        assertEquals("Java中如何定义类？", response.getBody());
        assertEquals(3, response.getDifficulty());
    }

    @Test
    void createQuestion_CourseNotFound() {
        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            questionBankService.createQuestion(questionRequest, teacherId);
        });

        assertEquals("课程不存在", exception.getMessage());
    }

    @Test
    void createQuestion_NoPermission() {
        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(courseId);
        otherCourse.setTeacherId(2L); // 其他教师

        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(otherCourse));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.createQuestion(questionRequest, teacherId);
        });

        assertEquals("您没有权限在此课程下创建题目", exception.getMessage());
    }

    @Test
    void createQuestion_KnowledgePointNotFound() {
        // 设置无效的知识点ID
        questionRequest.setKnowledgePointId(999L);

        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(knowledgePointRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            questionBankService.createQuestion(questionRequest, teacherId);
        });

        assertEquals("知识点不存在", exception.getMessage());
    }



    @Test
    void getQuestions_Success() {
        // 准备测试数据
        QuestionFilterRequest filterRequest = new QuestionFilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);
        filterRequest.setCourseId(courseId);

        // 创建分页数据
        Page<Question> page = new PageImpl<>(Collections.singletonList(question));

        // 模拟依赖行为
        when(questionRepository.findByConditions(
                eq(courseId), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)
        )).thenReturn(page);

        // 执行测试
        Page<QuestionResponse> result = questionBankService.getQuestions(filterRequest, teacherId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        QuestionResponse response = result.getContent().get(0);
        assertEquals(questionId, response.getId());
    }

    @Test
    void getQuestions_WithAllFilters() {
        // 准备测试数据
        QuestionFilterRequest filterRequest = new QuestionFilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);
        filterRequest.setCourseId(courseId);
        filterRequest.setKnowledgePointId(knowledgePointId);
        filterRequest.setType("SINGLE_CHOICE");
        filterRequest.setDifficulty(3);
        filterRequest.setKeyword("Java");

        // 创建分页数据
        Page<Question> page = new PageImpl<>(Collections.singletonList(question));

        // 模拟依赖行为
        when(questionRepository.findByConditions(
                eq(courseId), eq(knowledgePointId), eq(Question.QuestionType.SINGLE_CHOICE),
                eq(3), eq("Java"), any(Pageable.class)
        )).thenReturn(page);

        // 执行测试
        Page<QuestionResponse> result = questionBankService.getQuestions(filterRequest, teacherId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getQuestions_InvalidType() {
        // 准备测试数据
        QuestionFilterRequest filterRequest = new QuestionFilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);
        filterRequest.setType("INVALID_TYPE");

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.getQuestions(filterRequest, teacherId);
        });

        assertEquals("无效的题目类型: INVALID_TYPE", exception.getMessage());
    }

    @Test
    void getQuestionById_Success() {
        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        // 执行测试
        QuestionResponse response = questionBankService.getQuestionById(questionId, teacherId);

        // 验证结果
        assertNotNull(response);
        assertEquals(questionId, response.getId());
    }

    @Test
    void getQuestionById_NotFound() {
        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            questionBankService.getQuestionById(questionId, teacherId);
        });

        assertEquals("题目不存在", exception.getMessage());
    }

    @Test
    void getQuestionById_NoPermission() {
        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(courseId);
        otherCourse.setTeacherId(2L); // 其他教师

        // 创建题目
        Question otherQuestion = new Question();
        otherQuestion.setId(questionId);
        otherQuestion.setCourse(otherCourse);

        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(otherQuestion));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.getQuestionById(questionId, teacherId);
        });

        assertEquals("您没有权限查看此题目", exception.getMessage());
    }

    @Test
    void updateQuestion_Success() {
        // 准备更新数据
        QuestionRequest updateRequest = new QuestionRequest();
        updateRequest.setCourseId(courseId);
        updateRequest.setKnowledgePointId(knowledgePointId);
        updateRequest.setType("MULTI_CHOICE");
        updateRequest.setBody("Java中如何定义接口？");
        updateRequest.setDifficulty(4);

        // 创建更新后的题目
        Question updatedQuestion = new Question();
        updatedQuestion.setId(questionId);
        updatedQuestion.setCourse(course);
        updatedQuestion.setKnowledgePoint(knowledgePoint);
        updatedQuestion.setType(Question.QuestionType.MULTI_CHOICE);
        updatedQuestion.setBody("Java中如何定义接口？");
        updatedQuestion.setDifficulty(4);

        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(knowledgePointRepository.findById(knowledgePointId)).thenReturn(Optional.of(knowledgePoint));
        when(questionRepository.save(any(Question.class))).thenReturn(updatedQuestion);

        // 执行测试
        QuestionResponse response = questionBankService.updateQuestion(questionId, updateRequest, teacherId);

        // 验证结果
        assertNotNull(response);
        assertEquals(questionId, response.getId());
        assertEquals("MULTI_CHOICE", response.getType());
        assertEquals("Java中如何定义接口？", response.getBody());
        assertEquals(4, response.getDifficulty());
    }

    @Test
    void updateQuestion_NotFound() {
        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            questionBankService.updateQuestion(questionId, questionRequest, teacherId);
        });

        assertEquals("题目不存在", exception.getMessage());
    }

    @Test
    void updateQuestion_NoPermission() {
        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(courseId);
        otherCourse.setTeacherId(2L); // 其他教师

        // 创建题目
        Question otherQuestion = new Question();
        otherQuestion.setId(questionId);
        otherQuestion.setCourse(otherCourse);

        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(otherQuestion));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.updateQuestion(questionId, questionRequest, teacherId);
        });

        assertEquals("您没有权限修改此题目", exception.getMessage());
    }

    @Test
    void deleteQuestion_Success() {
        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        // 执行测试
        questionBankService.deleteQuestion(questionId, teacherId);

        // 验证结果
        verify(questionRepository).delete(question);
    }

    @Test
    void deleteQuestion_NotFound() {
        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            questionBankService.deleteQuestion(questionId, teacherId);
        });

        assertEquals("题目不存在", exception.getMessage());
    }

    @Test
    void deleteQuestion_NoPermission() {
        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(courseId);
        otherCourse.setTeacherId(2L); // 其他教师

        // 创建题目
        Question otherQuestion = new Question();
        otherQuestion.setId(questionId);
        otherQuestion.setCourse(otherCourse);

        // 模拟依赖行为
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(otherQuestion));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.deleteQuestion(questionId, teacherId);
        });

        assertEquals("您没有权限删除此题目", exception.getMessage());
    }

    @Test
    void deleteQuestions_Success() {
        // 准备测试数据
        List<Long> questionIds = Arrays.asList(questionId, 202L);

        // 创建题目列表
        List<Question> questions = Arrays.asList(
                question,
                createQuestion(202L, course, knowledgePoint)
        );

        // 模拟依赖行为
        when(questionRepository.findAllById(questionIds)).thenReturn(questions);

        // 执行测试
        questionBankService.deleteQuestions(questionIds, teacherId);

        // 验证结果
        verify(questionRepository).deleteAll(questions);
    }

    @Test
    void deleteQuestions_NoPermission() {
        // 准备测试数据
        List<Long> questionIds = Arrays.asList(questionId, 202L);

        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(courseId);
        otherCourse.setTeacherId(2L); // 其他教师

        // 创建题目列表
        List<Question> questions = Arrays.asList(
                question,
                createQuestion(202L, otherCourse, knowledgePoint) // 其他教师的题目
        );

        // 模拟依赖行为
        when(questionRepository.findAllById(questionIds)).thenReturn(questions);

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.deleteQuestions(questionIds, teacherId);
        });

        assertEquals("您没有权限删除题目ID: 202", exception.getMessage());
    }

    @Test
    void getQuestionsByCourse_Success() {
        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(questionRepository.findByCourseCourseId(courseId)).thenReturn(Collections.singletonList(question));

        // 执行测试
        List<QuestionResponse> result = questionBankService.getQuestionsByCourse(courseId, teacherId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        QuestionResponse response = result.get(0);
        assertEquals(questionId, response.getId());
    }

    @Test
    void getQuestionsByCourse_CourseNotFound() {
        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            questionBankService.getQuestionsByCourse(courseId, teacherId);
        });

        assertEquals("课程不存在", exception.getMessage());
    }

    @Test
    void getQuestionsByCourse_NoPermission() {
        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(courseId);
        otherCourse.setTeacherId(2L); // 其他教师

        // 模拟依赖行为
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(otherCourse));

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            questionBankService.getQuestionsByCourse(courseId, teacherId);
        });

        assertEquals("您没有权限查看此课程的题目", exception.getMessage());
    }

    @Test
    void searchQuestionsByKeyword_Success() {
        // 准备测试数据
        String keyword = "Java";

        // 创建题目列表
        List<Question> questions = Arrays.asList(
                question,
                createQuestion(202L, course, knowledgePoint)
        );

        // 模拟依赖行为
        when(questionRepository.findByKeyword(keyword)).thenReturn(questions);

        // 执行测试
        List<QuestionResponse> result = questionBankService.searchQuestionsByKeyword(keyword, teacherId);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void searchQuestionsByKeyword_FilterByTeacher() {
        // 准备测试数据
        String keyword = "Java";

        // 创建其他教师的课程
        Course otherCourse = new Course();
        otherCourse.setCourseId(102L);
        otherCourse.setTeacherId(2L); // 其他教师

        // 创建题目列表
        List<Question> questions = Arrays.asList(
                question, // 当前教师的题目
                createQuestion(202L, otherCourse, knowledgePoint) // 其他教师的题目
        );

        // 模拟依赖行为
        when(questionRepository.findByKeyword(keyword)).thenReturn(questions);

        // 执行测试
        List<QuestionResponse> result = questionBankService.searchQuestionsByKeyword(keyword, teacherId);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(questionId, result.get(0).getId());
    }

    // 辅助方法：创建题目
    private Question createQuestion(Long id, Course course, KnowledgePoint knowledgePoint) {
        Question newQuestion = new Question();
        newQuestion.setId(id);
        newQuestion.setCourse(course);
        newQuestion.setKnowledgePoint(knowledgePoint);
        newQuestion.setType(Question.QuestionType.SINGLE_CHOICE);
        newQuestion.setBody("问题 " + id);
        newQuestion.setDifficulty(3);
        return newQuestion;
    }
}