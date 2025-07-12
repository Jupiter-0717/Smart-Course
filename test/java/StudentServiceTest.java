package com.example.smartlearn.service.teacher;

import com.example.smartlearn.dto.StudentDTO;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.Student;
import com.example.smartlearn.model.StudentCourse;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.repository.StudentCourseRepository;
import com.example.smartlearn.repository.StudentRepository;
import com.example.smartlearn.repository.UserRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceTest {

    @InjectMocks
    private StudentService studentService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateOrLinkStudent_NewStudent() {
        Long studentId = 1L;
        Long courseId = 100L;
        String studentName = "Alice";

        Course course = new Course();
        course.setCourseId(courseId);

        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArguments()[0]);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(studentCourseRepository.findByCourseIdAndStudentId(courseId, studentId)).thenReturn(Optional.empty());

        Student student = studentService.createOrLinkStudent(studentName, studentId, courseId);

        assertNotNull(student);
        assertEquals(studentId, student.getStudentId());
        assertEquals(studentName, student.getStudentName());
    }

    @Test
    void testCreateOrLinkStudent_AlreadyLinked() {
        Long studentId = 1L;
        Long courseId = 100L;
        String studentName = "Alice";

        Student student = new Student();
        Course course = new Course();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentCourseRepository.findByCourseIdAndStudentId(courseId, studentId))
                .thenReturn(Optional.of(new StudentCourse()));

        assertThrows(IllegalArgumentException.class, () ->
                studentService.createOrLinkStudent(studentName, studentId, courseId));
    }

    @Test
    void testDeleteStudent_NotEnrolled() {
        Long studentId = 1L;
        Long courseId = 200L;

        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId))
                .thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                studentService.deleteStudent(studentId, courseId));
    }

    @Test
    void testDeleteStudent_Success() {
        Long studentId = 1L;
        Long courseId = 200L;

        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId))
                .thenReturn(true);

        studentService.deleteStudent(studentId, courseId);

        verify(studentCourseRepository).deleteByStudentStudentIdAndCourseCourseId(studentId, courseId);
    }

    @Test
    void testGetStudentById_Valid() {
        Long studentId = 1L;
        Long courseId = 200L;
        Student student = new Student();

        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId)).thenReturn(true);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Optional<Student> result = studentService.getStudentById(studentId, courseId);
        assertTrue(result.isPresent());
        assertEquals(student, result.get());
    }

    @Test
    void testGetStudentsByCourseId() {
        Long courseId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        Student student = new Student();
        student.setStudentId(1L);
        student.setStudentName("Alice");
        Page<Student> page = new PageImpl<>(List.of(student));

        when(studentRepository.findByCourseId(courseId, pageable)).thenReturn(page);

        Page<StudentDTO> result = studentService.getStudentsByCourseId(courseId, pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("Alice", result.getContent().get(0).getStudentName());
    }

    @Test
    void testExportStudentByCourse() {
        Long courseId = 1L;
        Student student = new Student();
        student.setStudentId(1L);
        student.setStudentName("Bob");
        Page<Student> page = new PageImpl<>(List.of(student));

        when(studentRepository.findByCourse_Id(courseId, Pageable.unpaged())).thenReturn(page);

        List<StudentDTO> dtos = studentService.exportStudentByCourse(courseId);
        assertEquals(1, dtos.size());
        assertEquals("Bob", dtos.get(0).getStudentName());
    }
}
