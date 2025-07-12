package com.example.smartlearn.service.stuednt;

import com.example.smartlearn.dto.request.QuizTaskListRequest;
import com.example.smartlearn.dto.request.QuizSubmissionListRequest;
import com.example.smartlearn.dto.request.QuizSubmissionDetailRequest;
import com.example.smartlearn.dto.request.SubmitQuizRequest;
import com.example.smartlearn.dto.response.QuizTaskListResponse;
import com.example.smartlearn.dto.response.QuizSubmissionListResponse;
import com.example.smartlearn.dto.response.QuizResponse;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.*;
import com.example.smartlearn.service.student.StudentQuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentQuizServiceTest {
    @Mock QuizRepository quizRepository;
    @Mock StudentRepository studentRepository;
    @Mock TaskRepository taskRepository;
    @Mock QuestionRepository questionRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock StudentAnswerRepository studentAnswerRepository;
    @Mock StudentCourseRepository studentCourseRepository;
    @InjectMocks
    StudentQuizService studentQuizService;

    Long testStudentId = 1L;
    Long testCourseId = 1L;
    Long testTaskId = 1L;
    Long testQuizId = 1L;
    Long testSubmissionId = 1L;
    Long testQuestionId = 1L;
    Student testStudent;
    Course testCourse;
    Task testTask;
    Quiz testQuiz;
    Question testQuestion;
    Submission testSubmission;
    StudentCourse testStudentCourse;

    @BeforeEach
    void setUp() {
        testStudent = new Student();
        testStudent.setStudentId(testStudentId);
        testStudent.setStudentName("Test Student");
        testCourse = new Course();
        testCourse.setCourseId(testCourseId);
        testCourse.setName("Test Course");
        testCourse.setCode("CS101");
        testTask = new Task();
        testTask.setId(testTaskId);
        testTask.setTitle("Test Quiz Task");
        testTask.setType(Task.TaskType.QUIZ);
        testTask.setCourse(testCourse);
        testTask.setDueDate(LocalDateTime.now().plusDays(1));
        testQuiz = new Quiz();
        testQuiz.setId(testQuizId);
        testQuiz.setTitle("Test Quiz");
        testQuiz.setCourse(testCourse);
        testQuestion = new Question();
        testQuestion.setId(testQuestionId);
        testQuestion.setType(Question.QuestionType.SINGLE_CHOICE);
        testQuestion.setBody("{\"content\":\"What is 2+2?\",\"options\":\"A.3,B.4,C.5,D.6\",\"correctAnswer\":\"B\",\"score\":10}");
        testQuiz.setQuestions(List.of(testQuestion));
        testSubmission = new Submission();
        testSubmission.setId(testSubmissionId);
        testSubmission.setTask(testTask);
        testSubmission.setStudent(testStudent);
        testSubmission.setSubmittedAt(LocalDateTime.now());
        testSubmission.setGrade(new BigDecimal("85.0"));
        testStudentCourse = new StudentCourse();
        testStudentCourse.setStudent(testStudent);
        testStudentCourse.setCourse(testCourse);
    }

    @Test
    void getStudentQuizTasks_StudentNotFound() {
        QuizTaskListRequest request = new QuizTaskListRequest();
        request.setStudentId(testStudentId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> studentQuizService.getStudentQuizTasks(request));
    }

    @Test
    void getStudentQuizTasks_NoCourses() {
        QuizTaskListRequest request = new QuizTaskListRequest();
        request.setStudentId(testStudentId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(studentCourseRepository.findByStudentStudentId(testStudentId)).thenReturn(Collections.emptyList());
        QuizTaskListResponse resp = studentQuizService.getStudentQuizTasks(request);
        assertEquals(0, resp.getTotal());
        assertTrue(resp.getQuizTasks().isEmpty());
    }

    @Test
    void getStudentQuizTasks_Success() {
        QuizTaskListRequest request = new QuizTaskListRequest();
        request.setStudentId(testStudentId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(studentCourseRepository.findByStudentStudentId(testStudentId)).thenReturn(List.of(testStudentCourse));
        when(taskRepository.findByCourseCourseIdInAndType(anyList(), eq(Task.TaskType.QUIZ))).thenReturn(List.of(testTask));
        when(submissionRepository.findByStudentStudentIdAndTaskId(testStudentId, testTaskId)).thenReturn(Collections.emptyList());
        QuizTaskListResponse resp = studentQuizService.getStudentQuizTasks(request);
        assertEquals(1, resp.getTotal());
        assertEquals(testTaskId, resp.getQuizTasks().get(0).getTaskId());
    }

    @Test
    void getStudentQuizSubmissions_StudentNotFound() {
        QuizSubmissionListRequest request = new QuizSubmissionListRequest();
        request.setStudentId(testStudentId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> studentQuizService.getStudentQuizSubmissions(request));
    }

    @Test
    void getStudentQuizSubmissions_NoCourses() {
        QuizSubmissionListRequest request = new QuizSubmissionListRequest();
        request.setStudentId(testStudentId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(studentCourseRepository.findByStudentStudentId(testStudentId)).thenReturn(Collections.emptyList());
        QuizSubmissionListResponse resp = studentQuizService.getStudentQuizSubmissions(request);
        assertEquals(0, resp.getTotal());
        assertTrue(resp.getSubmissions().isEmpty());
    }

    @Test
    void getQuizSubmissionDetail_StudentNotFound() {
        QuizSubmissionDetailRequest request = new QuizSubmissionDetailRequest();
        request.setStudentId(testStudentId);
        request.setSubmissionId(testSubmissionId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> studentQuizService.getQuizSubmissionDetail(request));
    }

    @Test
    void getQuizSubmissionDetail_SubmissionNotFound() {
        QuizSubmissionDetailRequest request = new QuizSubmissionDetailRequest();
        request.setStudentId(testStudentId);
        request.setSubmissionId(testSubmissionId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.of(testStudent));
        when(submissionRepository.findById(testSubmissionId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> studentQuizService.getQuizSubmissionDetail(request));
    }

    @Test
    void submitQuizAnswers_StudentNotFound() {
        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setStudentId(testStudentId);
        when(studentRepository.findById(testStudentId)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> studentQuizService.submitQuizAnswers(request));
        assertTrue(ex.getMessage().contains("学生不存在"));
        assertTrue(ex.getCause() instanceof ResourceNotFoundException);
    }

} 