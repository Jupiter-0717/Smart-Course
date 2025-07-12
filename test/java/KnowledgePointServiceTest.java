package com.example.smartlearn;

import com.example.smartlearn.dto.response.KnowledgePointResponse;
import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.*;
import com.example.smartlearn.service.teacher.Class_ResourceService;
import com.example.smartlearn.service.teacher.KnowledgePointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KnowledgePointServiceTest {

    @Mock
    private KnowledgePointRepository knowledgePointRepository;

    @Mock
    private Class_ResourceService class_ResourceService;

    @Mock
    private ClassResourceRepository classResourceRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private KnowledgePointResourceRepository knowledgePointResourceRepository;

    @InjectMocks
    private KnowledgePointService knowledgePointService;

    @Test
    public void whenGetByCourseId_thenReturnKnowledgePoints() {
        // 准备测试数据
        Long courseId = 1L;

        KnowledgePoint kp1 = new KnowledgePoint();
        kp1.setId(1L);
        kp1.setName("知识点1");
        kp1.setDescription("描述1");

        KnowledgePoint kp2 = new KnowledgePoint();
        kp2.setId(2L);
        kp2.setName("知识点2");
        kp2.setDescription("描述2");

        List<KnowledgePoint> mockPoints = Arrays.asList(kp1, kp2);

        // 模拟repository行为
        when(knowledgePointRepository.findByCourse_CourseId(courseId)).thenReturn(mockPoints);

        // 调用方法
        List<KnowledgePointResponse> result = knowledgePointService.getByCourseId(courseId);

        // 验证结果
        assertEquals(2, result.size());
        assertEquals("知识点1", result.get(0).getName());
        assertEquals("描述1", result.get(0).getDescription());
        verify(knowledgePointRepository).findByCourse_CourseId(courseId);
    }

    @Test
    public void whenGetByCourseIdWithNoPoints_thenReturnEmptyList() {
        Long courseId = 1L;

        when(knowledgePointRepository.findByCourse_CourseId(courseId)).thenReturn(Collections.emptyList());

        List<KnowledgePointResponse> result = knowledgePointService.getByCourseId(courseId);

        assertTrue(result.isEmpty());
        verify(knowledgePointRepository).findByCourse_CourseId(courseId);
    }

    @Test
    public void whenSearchByCourseIdAndName_thenReturnFilteredPoints() {
        // 准备测试数据
        Long courseId = 1L;
        String searchName = "数学";

        KnowledgePoint kp1 = new KnowledgePoint();
        kp1.setId(1L);
        kp1.setName("数学分析");
        kp1.setDescription("数学基础");

        KnowledgePoint kp2 = new KnowledgePoint();
        kp2.setId(2L);
        kp2.setName("高等数学");
        kp2.setDescription("高等数学基础");

        List<KnowledgePoint> mockPoints = Arrays.asList(kp1, kp2);

        // 模拟repository行为
        when(knowledgePointRepository.findByCourse_CourseIdAndNameContaining(courseId, searchName))
                .thenReturn(mockPoints);

        // 调用方法
        List<KnowledgePointResponse> result = knowledgePointService.searchByCourseIdAndName(courseId, searchName);

        // 验证结果
        assertEquals(2, result.size());
        assertTrue(result.get(0).getName().contains(searchName));
        verify(knowledgePointRepository).findByCourse_CourseIdAndNameContaining(courseId, searchName);
    }

    @Test
    public void whenSearchByCourseIdAndNameNoMatch_thenReturnEmptyList() {
        Long courseId = 1L;
        String searchName = "不存在的知识点";

        when(knowledgePointRepository.findByCourse_CourseIdAndNameContaining(courseId, searchName))
                .thenReturn(Collections.emptyList());

        List<KnowledgePointResponse> result = knowledgePointService.searchByCourseIdAndName(courseId, searchName);

        assertTrue(result.isEmpty());
        verify(knowledgePointRepository).findByCourse_CourseIdAndNameContaining(courseId, searchName);
    }
}