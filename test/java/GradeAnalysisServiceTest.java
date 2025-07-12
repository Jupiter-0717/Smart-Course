package com.example.smartlearn;


import com.example.smartlearn.dto.response.CourseGradeReportResponse;
import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.*;
import com.example.smartlearn.service.teacher.GradeAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeAnalysisServiceTest {

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private GradeAnalysisService gradeAnalysisService;

    private final Long studentId = 1L;
    private final Long courseId = 101L;
    private final Long taskId = 201L;
    private Student student;
    private Course course;
    private Task task;
    private Submission submission;
    private StudentCourse studentCourse;

    @BeforeEach
    void setUp() {
        // 创建学生
        student = new Student();
        student.setStudentId(studentId);
        student.setStudentName("张三");

        // 创建课程
        course = new Course();
        course.setCourseId(courseId);
        course.setName("计算机科学");
        course.setCode("CS101");

        // 创建任务
        task = new Task();
        task.setId(taskId);
        task.setCourse(course);
        task.setTitle("期中考试");
        task.setType(Task.TaskType.QUIZ);

        // 创建提交
        submission = new Submission();
        submission.setId(1L);
        submission.setStudent(student);
        submission.setTask(task);
        submission.setGrade(BigDecimal.valueOf(85));
        submission.setSubmittedAt(LocalDateTime.now());

        // 创建学生课程关联
        studentCourse = new StudentCourse();
        studentCourse.setStudent(student);
        studentCourse.setCourse(course);
        studentCourse.setFinalGrade("85.0");
    }



    @Test
    void getTaskMaxScore_Quiz() throws Exception {
        // 准备测试数据
        Quiz quiz = new Quiz();
        quiz.setTotalPoints(50);
        task.setQuiz(quiz);

        // 使用反射调用私有方法
        Method method = GradeAnalysisService.class.getDeclaredMethod("getTaskMaxScore", Task.class);
        method.setAccessible(true);
        BigDecimal result = (BigDecimal) method.invoke(gradeAnalysisService, task);

        // 验证结果
        assertEquals(BigDecimal.valueOf(50), result);
    }

    @Test
    void getTaskMaxScore_Default() throws Exception {
        // 使用反射调用私有方法
        Method method = GradeAnalysisService.class.getDeclaredMethod("getTaskMaxScore", Task.class);
        method.setAccessible(true);
        BigDecimal result = (BigDecimal) method.invoke(gradeAnalysisService, task);

        // 验证结果
        assertEquals(BigDecimal.valueOf(100), result);
    }

    @Test
    void getStudentFinalGrade_FromStudentCourse() throws Exception {
        // 准备测试数据
        studentCourse.setFinalGrade("90.0");

        // 模拟依赖行为
        when(studentCourseRepository.findByCourseIdAndStudentId(courseId, studentId))
                .thenReturn(Optional.of(studentCourse));

        // 使用反射调用私有方法
        Method method = GradeAnalysisService.class.getDeclaredMethod("getStudentFinalGrade", Long.class, Long.class);
        method.setAccessible(true);
        BigDecimal result = (BigDecimal) method.invoke(gradeAnalysisService, studentId, courseId);

        // 验证结果
        assertEquals(BigDecimal.valueOf(90.0), result);
    }

    @Test
    void getStudentFinalGrade_NotAvailable() throws Exception {
        // 模拟依赖行为
        when(studentCourseRepository.findByCourseIdAndStudentId(courseId, studentId))
                .thenReturn(Optional.empty());

        // 使用反射调用私有方法
        Method method = GradeAnalysisService.class.getDeclaredMethod("getStudentFinalGrade", Long.class, Long.class);
        method.setAccessible(true);
        BigDecimal result = (BigDecimal) method.invoke(gradeAnalysisService, studentId, courseId);

        // 验证结果
        assertNull(result);
    }







    @Test
    void getGradeLevel_High() throws Exception {
        // 使用反射调用私有方法
        Method method = GradeAnalysisService.class.getDeclaredMethod("getGradeLevel", double.class);
        method.setAccessible(true);

        // 验证各种成绩等级
        assertEquals("优秀", method.invoke(gradeAnalysisService, 95.0));
        assertEquals("良好", method.invoke(gradeAnalysisService, 85.0));
        assertEquals("中等", method.invoke(gradeAnalysisService, 75.0));
        assertEquals("及格", method.invoke(gradeAnalysisService, 65.0));
        assertEquals("不及格", method.invoke(gradeAnalysisService, 55.0));
    }



    @Test
    void calculateTaskStatistics_Success() throws Exception {
        // 准备测试数据
        List<Task> tasks = Collections.singletonList(task);

        List<StudentCourse> studentCourses = Arrays.asList(
                createStudentCourse(1L, 90.0),
                createStudentCourse(2L, 85.0),
                createStudentCourse(3L, 75.0)
        );

        // 模拟提交
        when(submissionRepository.findByStudentStudentIdAndTaskId(1L, taskId))
                .thenReturn(Collections.singletonList(createSubmission(1L, BigDecimal.valueOf(90))));
        when(submissionRepository.findByStudentStudentIdAndTaskId(2L, taskId))
                .thenReturn(Collections.singletonList(createSubmission(2L, BigDecimal.valueOf(85))));
        when(submissionRepository.findByStudentStudentIdAndTaskId(3L, taskId))
                .thenReturn(Collections.singletonList(createSubmission(3L, BigDecimal.valueOf(75))));

        // 使用反射调用私有方法
        Method method = GradeAnalysisService.class.getDeclaredMethod("calculateTaskStatistics",
                List.class, List.class);
        method.setAccessible(true);
        List<CourseGradeReportResponse.TaskStatistics> result =
                (List<CourseGradeReportResponse.TaskStatistics>) method.invoke(gradeAnalysisService, tasks, studentCourses);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        CourseGradeReportResponse.TaskStatistics taskStat = result.get(0);
        assertEquals(taskId, taskStat.getTaskId());
        assertEquals(3, taskStat.getTotalStudents());
        assertEquals(3, taskStat.getSubmittedStudents());
        assertEquals(3, taskStat.getCompletedStudents());
        assertEquals(BigDecimal.valueOf(83.33), taskStat.getAverageScore()); // (90+85+75)/3
        assertEquals(83.33, taskStat.getAveragePercentage(), 0.01);
        assertEquals(100.0, taskStat.getCompletionRate());
    }

    // 辅助方法：创建任务
    private Task createTask(Long id, String title, Task.TaskType type, int maxScore) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setType(type);
        task.setCourse(course);

        if (type == Task.TaskType.QUIZ) {
            Quiz quiz = new Quiz();
            quiz.setTotalPoints(maxScore);
            task.setQuiz(quiz);
        }

        return task;
    }

    // 辅助方法：创建提交
    private Submission createSubmission(Long id, BigDecimal grade) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setGrade(grade);
        submission.setTask(task);
        return submission;
    }

    // 辅助方法：创建学生课程关联
    private StudentCourse createStudentCourse(Long studentId, double finalGrade) {
        Student student = new Student();
        student.setStudentId(studentId);

        StudentCourse studentCourse = new StudentCourse();
        studentCourse.setStudent(student);
        studentCourse.setCourse(course);
        studentCourse.setFinalGrade(String.valueOf(finalGrade));
        return studentCourse;
    }

    // 辅助方法：创建学生成绩信息
    private CourseGradeReportResponse.StudentGradeInfo createStudentGradeInfo(double grade, String gradeLevel) {
        CourseGradeReportResponse.StudentGradeInfo info = new CourseGradeReportResponse.StudentGradeInfo();
        info.setTotalGrade(BigDecimal.valueOf(grade));
        info.setGradeLevel(gradeLevel);
        return info;
    }
}