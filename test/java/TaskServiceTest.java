package com.example.smartlearn.service.teacher;

import com.example.smartlearn.dto.request.TaskRequest;
import com.example.smartlearn.dto.response.TaskResponse;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.Quiz;
import com.example.smartlearn.model.Task;
import com.example.smartlearn.model.Teacher;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.repository.QuizRepository;
import com.example.smartlearn.repository.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private QuizRepository quizRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateTask_success_withQuiz() {
        TaskRequest request = new TaskRequest();
        request.setCourseId(1L);
        request.setTitle("Test Task");
        request.setDescription("Desc");
        request.setType("QUIZ");
        request.setQuizId(100L);
        request.setDueDate(LocalDateTime.now());

        Course course = new Course();
        course.setCourseId(1L);

        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.setCourse(course);

        Task savedTask = new Task();
        savedTask.setId(10L);
        savedTask.setTitle("Test Task");
        savedTask.setDescription("Desc");
        savedTask.setType(Task.TaskType.QUIZ);
        savedTask.setDueDate(request.getDueDate());
        savedTask.setCourse(course);
        savedTask.setQuiz(quiz);
        savedTask.setCreatedAt(LocalDateTime.now());

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(quizRepository.findById(100L)).thenReturn(Optional.of(quiz));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
        assertEquals("QUIZ", response.getType());
        assertEquals(100L, response.getQuizId());
    }

    @Test
    void testCreateTask_fail_courseNotFound() {
        TaskRequest request = new TaskRequest();
        request.setCourseId(999L);

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.createTask(request));
    }

    @Test
    void testCreateTask_fail_quizNotMatchCourse() {
        TaskRequest request = new TaskRequest();
        request.setCourseId(1L);
        request.setQuizId(100L);
        request.setType("QUIZ");

        Course course = new Course();
        course.setCourseId(1L);

        Course wrongCourse = new Course();
        wrongCourse.setCourseId(2L);

        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.setCourse(wrongCourse);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(quizRepository.findById(100L)).thenReturn(Optional.of(quiz));

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request));
    }

    @Test
    void testGetTasksByCourse_success() {
        Long courseId = 1L;
        Long teacherId = 2L;

        Course course = new Course();
        course.setCourseId(courseId);
        course.setTeacherId(teacherId);

        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test");
        task.setDescription("desc");
        task.setType(Task.TaskType.HOMEWORK);
        task.setDueDate(LocalDateTime.now());
        task.setCourse(course);
        task.setCreatedAt(LocalDateTime.now());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(taskRepository.findByCourseCourseId(courseId)).thenReturn(List.of(task));

        List<TaskResponse> responses = taskService.getTasksByCourse(courseId, teacherId);
        assertEquals(1, responses.size());
        assertEquals("Test", responses.get(0).getTitle());
    }

    @Test
    void testGetTasksByCourse_unauthorized() {
        Long courseId = 1L;
        Long teacherId = 2L;

        Course course = new Course();
        course.setCourseId(courseId);
        course.setTeacherId(999L);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(SecurityException.class, () -> taskService.getTasksByCourse(courseId, teacherId));
    }

    @Test
    void testDeleteTask_success() {
        Task task = new Task();
        task.setId(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L);
        verify(taskRepository).delete(task);
    }

    @Test
    void testDeleteTask_notFound() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(1L));
    }

    @Test
    void testGetAvailableQuizzes_success() {
        Long teacherId = 1L;
        Long courseId = 2L;

        Course course = new Course();
        course.setCourseId(courseId);
        course.setTeacherId(teacherId);

        Quiz quiz = new Quiz();
        quiz.setId(100L);
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherId);
        quiz.setCreator(teacher);
        quiz.setCourse(course);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(quizRepository.findAllByCourseCourseId(courseId)).thenReturn(List.of(quiz));

        var result = taskService.getAvailableQuizzes(teacherId, courseId);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void testGetAvailableQuizzes_unauthorized() {
        Long teacherId = 1L;
        Long courseId = 2L;

        Course course = new Course();
        course.setCourseId(courseId);
        course.setTeacherId(999L); // 不匹配

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(SecurityException.class, () -> taskService.getAvailableQuizzes(teacherId, courseId));
    }
}
