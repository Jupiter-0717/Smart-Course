package com.example.smartlearn;

import com.example.smartlearn.dto.KnowledgePointDTO;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.KnowledgePoint;
import com.example.smartlearn.repository.ClassResourceRepository;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.repository.KnowledgePointRepository;
import com.example.smartlearn.repository.KnowledgePointResourceRepository;
import com.example.smartlearn.service.teacher.Class_ResourceService;
import com.example.smartlearn.service.teacher.KnowledgeGraphService;
import com.example.smartlearn.service.teacher.KnowledgePointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private KnowledgePointRepository pointRepository;

    @Mock
    private KnowledgePointResourceRepository resourceRepo;

    @Mock
    private ClassResourceRepository classResourceRepo;

    @Mock
    private KnowledgePointService knowledgePointService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private Class_ResourceService class_ResourceService;

    @Mock
    private KnowledgePointResourceRepository knowledgePointResourceRepository;

    @Mock
    private KnowledgePointRepository knowledgePointRepository;

    @Mock
    private ClassResourceRepository classResourceRepository;

    @InjectMocks
    private KnowledgeGraphService knowledgeGraphService;
    @Test
    void createKnowledgePoint_ShouldReturnPointId() {
        // 准备测试数据
        KnowledgePointDTO dto = new KnowledgePointDTO();
        dto.setName("测试知识点");
        dto.setDescription("测试描述");
        dto.setCourseId(1L);
        dto.setPositionX(100.0);
        dto.setPositionY(200.0);

        Course course = new Course();
        course.setCourseId(1L);

        KnowledgePoint savedPoint = new KnowledgePoint();
        savedPoint.setId(123L);
        savedPoint.setName(dto.getName());
        savedPoint.setDescription(dto.getDescription());
        savedPoint.setCourse(course);
        savedPoint.setPositionX(dto.getPositionX());
        savedPoint.setPositionY(dto.getPositionY());

        // 定义mock行为
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(pointRepository.save(any(KnowledgePoint.class))).thenReturn(savedPoint);
        Long pointId = knowledgeGraphService.createKnowledgePoint(dto);

        // 验证结果
        assertEquals(123L, pointId);

    }
    // 测试方法将在这里编写
}