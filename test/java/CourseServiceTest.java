package com.example.smartlearn;


import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.service.teacher.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private Course testCourse;
    private final Long testCourseId = 1L;
    private final Long testTeacherId = 100L;

    @BeforeEach
    void setUp() {
        // 创建测试课程
        testCourse = new Course();
        testCourse.setCourseId(testCourseId);
        testCourse.setCode("CS101");
        testCourse.setName("Computer Science");
        testCourse.setDescription("Introduction to Computer Science");
        testCourse.setCredit(3);
        testCourse.setTerm("Fall 2023");
        testCourse.setTeacherId(testTeacherId);
    }

    @Test
    void createCourse_Success() {
        // 准备测试数据
        Course newCourse = new Course();
        newCourse.setCode("MATH101");
        newCourse.setName("Mathematics");

        // 模拟依赖行为
        when(courseRepository.save(newCourse)).thenReturn(testCourse);

        // 执行测试
        Course result = courseService.createCourse(newCourse);

        // 验证结果
        assertNotNull(result);
        assertEquals(testCourseId, result.getCourseId());
        verify(courseRepository).save(newCourse);
    }

    @Test
    void getAllCourses_Success() {
        // 准备测试数据
        List<Course> courses = Arrays.asList(
                testCourse,
                createCourse(2L, "MATH101", "Mathematics", "Algebra and Calculus", 4, "Fall 2023", testTeacherId)
        );

        // 模拟依赖行为
        when(courseRepository.findAll()).thenReturn(courses);

        // 执行测试
        List<Course> result = courseService.getAllCourses();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Computer Science", result.get(0).getName());
        assertEquals("Mathematics", result.get(1).getName());
    }

    @Test
    void getCourseById_Success() {
        // 模拟依赖行为
        when(courseRepository.findById(testCourseId)).thenReturn(Optional.of(testCourse));

        // 执行测试
        Optional<Course> result = courseService.getCourseById(testCourseId);

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals(testCourseId, result.get().getCourseId());
        assertEquals("Computer Science", result.get().getName());
    }

    @Test
    void getCourseById_NotFound() {
        // 模拟依赖行为
        when(courseRepository.findById(testCourseId)).thenReturn(Optional.empty());

        // 执行测试
        Optional<Course> result = courseService.getCourseById(testCourseId);

        // 验证结果
        assertFalse(result.isPresent());
    }

    @Test
    void updateCourse_Success() {
        // 准备测试数据
        Course updatedCourse = new Course();
        updatedCourse.setName("Updated Computer Science");
        updatedCourse.setDescription("Updated Introduction to Computer Science");
        updatedCourse.setCredit(4);
        updatedCourse.setTerm("Spring 2024");

        // 模拟依赖行为
        when(courseRepository.findById(testCourseId)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(testCourse)).thenReturn(testCourse);

        // 执行测试
        Course result = courseService.updateCourse(testCourseId, updatedCourse);

        // 验证结果
        assertNotNull(result);
        assertEquals(testCourseId, result.getCourseId());
        assertEquals("Updated Computer Science", result.getName());
        assertEquals("Updated Introduction to Computer Science", result.getDescription());
        assertEquals(4, result.getCredit());
        assertEquals("Spring 2024", result.getTerm());

        // 验证课程编号未被修改
        assertEquals("CS101", result.getCode());
    }

    @Test
    void updateCourse_NotFound() {
        // 准备测试数据
        Course updatedCourse = new Course();
        updatedCourse.setName("Updated Computer Science");

        // 模拟依赖行为
        when(courseRepository.findById(testCourseId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            courseService.updateCourse(testCourseId, updatedCourse);
        });

        assertEquals("Course not found", exception.getMessage());
    }

    @Test
    void deleteCourse_Success() {
        // 执行测试
        courseService.deleteCourse(testCourseId);

        // 验证结果
        verify(courseRepository).deleteById(testCourseId);
    }

    @Test
    void getCoursesByTeacherPaged_Success() {
        // 准备测试数据
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(
                testCourse,
                createCourse(2L, "MATH101", "Mathematics", "Algebra and Calculus", 4, "Fall 2023", testTeacherId)
        );
        Page<Course> page = new PageImpl<>(courses, pageable, courses.size());

        // 模拟依赖行为
        when(courseRepository.findByTeacherId(testTeacherId, pageable)).thenReturn(page);

        // 执行测试
        Page<Course> result = courseService.getCoursesByTeacherPaged(testTeacherId, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("Computer Science", result.getContent().get(0).getName());
        assertEquals("Mathematics", result.getContent().get(1).getName());
    }

    @Test
    void searchCourses_WithKeyword_Success() {
        // 准备测试数据
        String keyword = "computer";
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Collections.singletonList(testCourse);
        Page<Course> page = new PageImpl<>(courses, pageable, courses.size());

        // 模拟依赖行为
        when(courseRepository.findByTeacherIdAndKeyword(testTeacherId, keyword, pageable)).thenReturn(page);

        // 执行测试
        Page<Course> result = courseService.searchCourses(testTeacherId, keyword, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Computer Science", result.getContent().get(0).getName());
    }

    @Test
    void searchCourses_EmptyKeyword() {
        // 准备测试数据
        String keyword = "";
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(
                testCourse,
                createCourse(2L, "MATH101", "Mathematics", "Algebra and Calculus", 4, "Fall 2023", testTeacherId)
        );
        Page<Course> page = new PageImpl<>(courses, pageable, courses.size());

        // 模拟依赖行为
        when(courseRepository.findByTeacherId(testTeacherId, pageable)).thenReturn(page);

        // 执行测试
        Page<Course> result = courseService.searchCourses(testTeacherId, keyword, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void searchCourses_InvalidKeyword() {
        // 准备测试数据
        String keyword = "!@#$%^&*()";
        Pageable pageable = PageRequest.of(0, 10);
        List<Course> courses = Arrays.asList(
                testCourse,
                createCourse(2L, "MATH101", "Mathematics", "Algebra and Calculus", 4, "Fall 2023", testTeacherId)
        );
        Page<Course> page = new PageImpl<>(courses, pageable, courses.size());

        // 模拟依赖行为
        when(courseRepository.findByTeacherId(testTeacherId, pageable)).thenReturn(page);

        // 执行测试
        Page<Course> result = courseService.searchCourses(testTeacherId, keyword, pageable);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getCoursesByTeacher_Success() {
        // 准备测试数据
        List<Course> courses = Arrays.asList(
                testCourse,
                createCourse(2L, "MATH101", "Mathematics", "Algebra and Calculus", 4, "Fall 2023", testTeacherId)
        );

        // 模拟依赖行为
        when(courseRepository.findByTeacherId(testTeacherId)).thenReturn(courses);

        // 执行测试
        List<Course> result = courseService.getCoursesByTeacher(testTeacherId);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Computer Science", result.get(0).getName());
        assertEquals("Mathematics", result.get(1).getName());
    }

    @Test
    void getCoursesByTeacher_Empty() {
        // 模拟依赖行为
        when(courseRepository.findByTeacherId(testTeacherId)).thenReturn(Collections.emptyList());

        // 执行测试
        List<Course> result = courseService.getCoursesByTeacher(testTeacherId);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 辅助方法：创建课程
    private Course createCourse(Long id, String code, String name, String description, int credit, String term, Long teacherId) {
        Course course = new Course();
        course.setCourseId(id);
        course.setCode(code);
        course.setName(name);
        course.setDescription(description);
        course.setCredit(credit);
        course.setTerm(term);
        course.setTeacherId(teacherId);
        return course;
    }
}