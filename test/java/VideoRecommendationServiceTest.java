package com.example.smartlearn;

import com.example.smartlearn.dto.VideoRecommendationDTO;
import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.*;
import com.example.smartlearn.service.video.VideoRecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoRecommendationServiceTest {

    @Mock
    private VideoResourceAnalyticsRepository videoRecommendationRepo;

    @Mock
    private VideoEventRepository videoEventRepo;

    @Mock
    private ClassResourceRepository classResourceRepo;

    @Mock
    private StudentCourseRepository studentCourseRepo;

    @InjectMocks
    private VideoRecommendationService videoRecommendationService;

    @Test
    public void whenRecommendVideosWithCourses_thenReturnRecommendations() {
        // 准备测试数据
        Long studentId = 1L;

        // 创建课程对象
        Course course1 = new Course();
        course1.setCourseId(101L);
        Course course2 = new Course();
        course2.setCourseId(102L);

        // 创建学生课程关联对象并设置课程
        StudentCourse studentCourse1 = new StudentCourse();
        studentCourse1.setCourse(course1); // 必须先设置Course对象

        StudentCourse studentCourse2 = new StudentCourse();
        studentCourse2.setCourse(course2); // 必须先设置Course对象

        List<StudentCourse> courses = Arrays.asList(studentCourse1, studentCourse2);
        // 模拟高质量视频
        ClassResource resource1 = new ClassResource();
        resource1.setResourceId(1L);
        resource1.setName("视频1");
        resource1.setUrl("url1");
        resource1.setDuration(120);

        ClassResource resource2 = new ClassResource();
        resource2.setResourceId(2L);
        resource2.setName("视频2");
        resource2.setUrl("url2");
        resource2.setDuration(180);

        VideoResourceAnalytics analytics1 = new VideoResourceAnalytics();
        analytics1.setResource(resource1);
        analytics1.setCompletionRate(0.8);

        VideoResourceAnalytics analytics2 = new VideoResourceAnalytics();
        analytics2.setResource(resource2);
        analytics2.setCompletionRate(0.9);

        // 模拟repository行为
        when(studentCourseRepo.findByStudentStudentId(studentId)).thenReturn(courses);
        when(videoRecommendationRepo.findHighQualityVideos(101L, PageRequest.of(0, 3)))
                .thenReturn(Collections.singletonList(analytics1));
        when(videoRecommendationRepo.findHighQualityVideos(102L, PageRequest.of(0, 3)))
                .thenReturn(Collections.singletonList(analytics2));

        // 调用方法
        List<VideoRecommendationDTO> result = videoRecommendationService.recommendVideos(studentId);

        // 验证结果
        assertEquals(2, result.size());
        assertEquals("视频2", result.get(0).getName()); // 按完成率排序
        assertEquals(0.9, result.get(0).getCompletionRate());
        assertEquals("根据您的学习效果推荐", result.get(0).getRecommendationReason());


    }
}