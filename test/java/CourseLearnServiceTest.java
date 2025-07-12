package com.example.smartlearn;


import com.example.smartlearn.dto.StudentCourseDTO;
import com.example.smartlearn.model.ClassResource;
import com.example.smartlearn.model.ClassResource.ResourceType;
import com.example.smartlearn.repository.ClassResourceRepository;
import com.example.smartlearn.repository.StudentCourseRepository;
import com.example.smartlearn.service.student.CourseLearnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseLearnServiceTest {

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private ClassResourceRepository classResourceRepository;

    @InjectMocks
    private CourseLearnService courseLearnService;

    @Test
    void getResourcesByCourseAndType_ValidAccess_ReturnsResources() throws AccessDeniedException {
        // 准备测试数据
        Long studentId = 1L;
        Long courseId = 100L;
        ResourceType resourceType = ResourceType.video; // 使用枚举类型

        // 模拟权限验证通过
        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId))
                .thenReturn(true);

        // 模拟资源查询结果
        ClassResource resource1 = createClassResource(1001L, ResourceType.video);
        ClassResource resource2 = createClassResource(1002L, ResourceType.video);

        // 使用正确的类型调用方法
        when(classResourceRepository.findByCourseIdAndType(courseId.intValue(), resourceType))
                .thenReturn(Arrays.asList(resource1, resource2));

        // 执行测试
        List<ClassResource> result = courseLearnService.getResourcesByCourseAndType(
                studentId, courseId, resourceType);

        // 验证结果
        assertEquals(2, result.size());
        assertEquals(1001L, result.get(0).getResourceId());
        assertEquals(1002L, result.get(1).getResourceId());
    }

    @Test
    void getResourcesByCourseAndType_InvalidAccess_ThrowsAccessDeniedException() {
        // 准备测试数据
        Long studentId = 1L;
        Long courseId = 100L;
        ResourceType resourceType = ResourceType.video; // 使用枚举类型

        // 模拟权限验证失败
        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId))
                .thenReturn(false);

        // 执行测试并验证异常
        assertThrows(AccessDeniedException.class, () -> {
            courseLearnService.getResourcesByCourseAndType(studentId, courseId, resourceType);
        });
    }

    @Test
    void getResourcesByCourseAndType_NoResources_ReturnsEmptyList() throws AccessDeniedException {
        // 准备测试数据
        Long studentId = 1L;
        Long courseId = 100L;
        ResourceType resourceType = ResourceType.doc; // 使用枚举类型

        // 模拟权限验证通过
        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId))
                .thenReturn(true);

        // 模拟空资源列表
        when(classResourceRepository.findByCourseIdAndType(courseId.intValue(), resourceType))
                .thenReturn(Collections.emptyList());

        // 执行测试
        List<ClassResource> result = courseLearnService.getResourcesByCourseAndType(
                studentId, courseId, resourceType);

        // 验证结果
        assertTrue(result.isEmpty());
    }



    @Test
    void getEnrolledCourse_NoCourses_ReturnsEmptyList() {
        // 准备测试数据
        Long studentId = 1L;

        // 模拟空课程列表
        when(studentCourseRepository.findCourseDTOsByStudentId(studentId))
                .thenReturn(Collections.emptyList());

        // 执行测试
        List<StudentCourseDTO> result = courseLearnService.getEnrolledCourse(studentId);

        // 验证结果
        assertTrue(result.isEmpty());
    }

    @Test
    void getEnrolledCourse_WithCourses_ReturnsCourseList() {
        // 准备测试数据
        Long studentId = 1L;
        List<StudentCourseDTO> mockCourses = Arrays.asList(
                createStudentCourseDTO(100L, "Java Programming"),
                createStudentCourseDTO(101L, "Database Systems")
        );

        // 模拟课程列表
        when(studentCourseRepository.findCourseDTOsByStudentId(studentId))
                .thenReturn(mockCourses);

        // 执行测试
        List<StudentCourseDTO> result = courseLearnService.getEnrolledCourse(studentId);

        // 验证结果
        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).getCourseId());
        assertEquals("Java Programming", result.get(0).getCourseName());
        assertEquals("CS100", result.get(0).getCode());
        assertEquals(101L, result.get(1).getCourseId());
        assertEquals("Database Systems", result.get(1).getCourseName());
        assertEquals("CS101", result.get(1).getCode());
    }

    @Test
    void getResourcesByCourseAndType_DifferentResourceTypes_ReturnsFilteredResources() throws AccessDeniedException {
        // 准备测试数据
        Long studentId = 1L;
        Long courseId = 100L;
        ResourceType resourceType = ResourceType.video; // 使用枚举类型

        // 模拟权限验证通过
        when(studentCourseRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId))
                .thenReturn(true);

        // 模拟资源查询结果（包含不同类型）
        ClassResource videoResource = createClassResource(1001L, ResourceType.video);
        ClassResource documentResource = createClassResource(1002L, ResourceType.doc);

        // 只返回视频资源
        when(classResourceRepository.findByCourseIdAndType(courseId.intValue(), resourceType))
                .thenReturn(Collections.singletonList(videoResource));

        // 执行测试
        List<ClassResource> result = courseLearnService.getResourcesByCourseAndType(
                studentId, courseId, resourceType);

        // 验证结果
        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getResourceId());
        assertEquals(ResourceType.video, result.get(0).getType());
    }

    // 辅助方法：创建ClassResource对象
    private ClassResource createClassResource(Long id, ResourceType type) {
        // 使用反射设置字段值
        try {
            ClassResource resource = new ClassResource();

            // 设置resourceId
            try {
                java.lang.reflect.Field idField = ClassResource.class.getDeclaredField("resourceId");
                idField.setAccessible(true);
                idField.set(resource, id);
            } catch (NoSuchFieldException e) {
                // 尝试其他可能的字段名
                trySetField(resource, "id", id);
                trySetField(resource, "resId", id);
            }

            // 设置resourceType
            try {
                java.lang.reflect.Field typeField = ClassResource.class.getDeclaredField("resourceType");
                typeField.setAccessible(true);
                typeField.set(resource, type);
            } catch (NoSuchFieldException e) {
                // 尝试其他可能的字段名
                trySetField(resource, "type", type);
                trySetField(resource, "resType", type);
            }

            return resource;
        } catch (Exception e) {
            throw new RuntimeException("创建ClassResource对象失败", e);
        }
    }

    // 通用字段设置方法
    private void trySetField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // 忽略错误
        }
    }

    // 辅助方法：创建StudentCourseDTO对象
    private StudentCourseDTO createStudentCourseDTO(Long courseId, String courseName) {
        return new StudentCourseDTO(courseId, "CS" + courseId, courseName);
    }
}