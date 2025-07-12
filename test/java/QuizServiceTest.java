package com.example.smartlearn.service.teacher;

import com.example.smartlearn.dto.request.QuizQuestionRequest;
import com.example.smartlearn.dto.request.QuizRequest;
import com.example.smartlearn.dto.response.QuizDetailResponse;
import com.example.smartlearn.dto.response.QuizResponse;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.Question;
import com.example.smartlearn.model.Quiz;
import com.example.smartlearn.model.Teacher;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.repository.QuestionRepository;
import com.example.smartlearn.repository.QuizRepository;
import com.example.smartlearn.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private QuizService quizService;

    private Teacher teacher;
    private Course course;
    private Quiz quiz;
    private Question question;

    @BeforeEach
    void setUp() {
        teacher = new Teacher();
        teacher.setTeacherId(1L);
        teacher.setName("张老师");
        course = new Course();
        course.setCourseId(2L);
        course.setName("数学");
        quiz = new Quiz();
        quiz.setId(3L);
        quiz.setTitle("期中测试");
        quiz.setCreator(teacher);
        quiz.setCourse(course);
        quiz.setTotalPoints(100);
        quiz.setQuestions(new ArrayList<>());
        question = new Question();
        question.setId(4L);
        question.setType(Question.QuestionType.SINGLE_CHOICE);
        question.setBody("1+1=?");
        question.setDifficulty(1);
    }

    @Test
    void testCreateQuiz_success() {
        QuizRequest request = new QuizRequest();
        request.setTitle("期末测试");
        request.setCreatorId(1L);
        request.setCourseId(2L);
        request.setTotalPoints(120);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz q = invocation.getArgument(0);
            q.setId(10L);
            return q;
        });
        QuizResponse response = quizService.createQuiz(request);
        assertNotNull(response);
        assertEquals("期末测试", response.getTitle());
        assertEquals(1L, response.getCreatorId());
        assertEquals(2L, response.getCourseId());
        assertEquals(120, response.getTotalPoints());
    }

    @Test
    void testCreateQuiz_teacherNotFound() {
        QuizRequest request = new QuizRequest();
        request.setTitle("期末测试");
        request.setCreatorId(99L);
        request.setCourseId(2L);
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> quizService.createQuiz(request));
        assertTrue(ex.getMessage().contains("教师不存在"));
    }

    @Test
    void testGetQuizById_success() {
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        QuizDetailResponse response = quizService.getQuizById(3L, 1L);
        assertNotNull(response);
        assertEquals(3L, response.getId());
        assertEquals("期中测试", response.getTitle());
    }

    @Test
    void testGetQuizById_notFound() {
        when(quizRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> quizService.getQuizById(100L, 1L));
    }

    @Test
    void testGetQuizById_noPermission() {
        Teacher other = new Teacher();
        other.setTeacherId(2L);
        quiz.setCreator(other);
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> quizService.getQuizById(3L, 1L));
        assertTrue(ex.getMessage().contains("没有权限"));
    }

    @Test
    void testUpdateQuiz_success() {
        QuizRequest request = new QuizRequest();
        request.setTitle("新标题");
        request.setTotalPoints(80);
        request.setCourseId(2L);
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        QuizResponse response = quizService.updateQuiz(3L, request, 1L);
        assertEquals("新标题", response.getTitle());
        assertEquals(80, response.getTotalPoints());
    }

    @Test
    void testUpdateQuiz_noPermission() {
        Teacher other = new Teacher();
        other.setTeacherId(2L);
        quiz.setCreator(other);
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        QuizRequest request = new QuizRequest();
        request.setTitle("新标题");
        assertThrows(IllegalArgumentException.class, () -> quizService.updateQuiz(3L, request, 1L));
    }

    @Test
    void testDeleteQuiz_success() {
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        doNothing().when(quizRepository).delete(any(Quiz.class));
        assertDoesNotThrow(() -> quizService.deleteQuiz(3L, 1L));
        verify(quizRepository).delete(quiz);
    }

    @Test
    void testDeleteQuiz_noPermission() {
        Teacher other = new Teacher();
        other.setTeacherId(2L);
        quiz.setCreator(other);
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        assertThrows(IllegalArgumentException.class, () -> quizService.deleteQuiz(3L, 1L));
    }

    @Test
    void testGetQuizzesByTeacher() {
        List<Quiz> quizList = Arrays.asList(quiz);
        when(quizRepository.findByCreatorTeacherId(1L)).thenReturn(quizList);
        List<QuizResponse> responses = quizService.getQuizzesByTeacher(1L);
        assertEquals(1, responses.size());
        assertEquals("期中测试", responses.get(0).getTitle());
    }

    @Test
    void testAddQuestionToQuiz_success() {
        quiz.setQuestions(new ArrayList<>());
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findById(4L)).thenReturn(Optional.of(question));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(quizRepository).updateQuestionScoreAndOrder(anyLong(), anyLong(), anyInt(), anyInt());
        QuizQuestionRequest request = new QuizQuestionRequest();
        request.setQuestionId(4L);
        request.setScore(5);
        request.setOrderIndex(1);
        quizService.addQuestionToQuiz(3L, request, 1L);
        assertTrue(quiz.getQuestions().contains(question));
    }

    @Test
    void testAddQuestionToQuiz_alreadyExists() {
        quiz.setQuestions(new ArrayList<>(Collections.singletonList(question)));
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findById(4L)).thenReturn(Optional.of(question));
        QuizQuestionRequest request = new QuizQuestionRequest();
        request.setQuestionId(4L);
        request.setScore(5);
        request.setOrderIndex(1);
        assertThrows(IllegalArgumentException.class, () -> quizService.addQuestionToQuiz(3L, request, 1L));
    }

    @Test
    void testRemoveQuestionFromQuiz_success() {
        quiz.setQuestions(new ArrayList<>(Collections.singletonList(question)));
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        quizService.removeQuestionFromQuiz(3L, 4L, 1L);
        assertFalse(quiz.getQuestions().contains(question));
    }

    @Test
    void testRemoveQuestionFromQuiz_notInQuiz() {
        quiz.setQuestions(new ArrayList<>());
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        assertThrows(ResourceNotFoundException.class, () -> quizService.removeQuestionFromQuiz(3L, 4L, 1L));
    }

    @Test
    void testSetQuestionSettings_success() {
        quiz.setQuestions(new ArrayList<>(Collections.singletonList(question)));
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        doNothing().when(quizRepository).updateQuestionScoreAndOrder(anyLong(), anyLong(), anyInt(), anyInt());
        QuizQuestionRequest request = new QuizQuestionRequest();
        request.setQuestionId(4L);
        request.setScore(10);
        request.setOrderIndex(2);
        assertDoesNotThrow(() -> quizService.setQuestionSettings(3L, request, 1L));
    }

    @Test
    void testSetQuestionSettings_notInQuiz() {
        quiz.setQuestions(new ArrayList<>());
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        QuizQuestionRequest request = new QuizQuestionRequest();
        request.setQuestionId(4L);
        assertThrows(IllegalArgumentException.class, () -> quizService.setQuestionSettings(3L, request, 1L));
    }

    @Test
    void testCopyQuiz_success() {
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz q = invocation.getArgument(0);
            q.setId(20L);
            return q;
        });
        QuizResponse response = quizService.copyQuiz(3L, "新副本", 1L);
        assertNotNull(response);
        assertEquals("新副本", response.getTitle());
        assertEquals(20L, response.getId());
    }

    @Test
    void testCopyQuiz_noPermission() {
        Teacher other = new Teacher();
        other.setTeacherId(2L);
        quiz.setCreator(other);
        when(quizRepository.findById(3L)).thenReturn(Optional.of(quiz));
        assertThrows(IllegalArgumentException.class, () -> quizService.copyQuiz(3L, "新副本", 1L));
    }

    @Test
    void testGetQuizzesByCourse() {
        List<Quiz> quizList = Arrays.asList(quiz);
        when(quizRepository.findAllByCourseCourseId(2L)).thenReturn(quizList);
        List<QuizResponse> responses = quizService.getQuizzesByCourse(2L);
        assertEquals(1, responses.size());
        assertEquals("期中测试", responses.get(0).getTitle());
    }
} 