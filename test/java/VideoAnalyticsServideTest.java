package com.example.smartlearn;

import com.example.smartlearn.dto.VideoAnalyticsDTO;
import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.*;
import com.example.smartlearn.service.video.VideoAnalyticsServide;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoAnalyticsServideTest {

    @Mock
    private VideoEventRepository videoEventRepository;

    @Mock
    private VideoResourceAnalyticsRepository videoResourceAnalyticsRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ClassResourceRepository classResourceRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private VideoAnalyticsServide videoAnalyticsService;
    @Test
    @Transactional
    public void testGenerateDetailAnalytics() {
        // 准备测试数据
        Object[] pair1 = new Object[]{1L, 101L};
        Object[] pair2 = new Object[]{2L, 102L};
        List<Object[]> studentResources = Arrays.asList(pair1, pair2);

        ClassResource resource1 = new ClassResource();
        resource1.setResourceId(101L);
        ClassResource resource2 = new ClassResource();
        resource2.setResourceId(102L);

        // 模拟repository行为
        when(videoEventRepository.findDistinctStudentResourcePairs()).thenReturn(studentResources);
        when(classResourceRepository.findAll()).thenReturn(Arrays.asList(resource1, resource2));
        when(videoResourceAnalyticsRepository.existsTodayAnalysis(anyLong(), anyLong())).thenReturn(false);

        // 调用方法
        videoAnalyticsService.generateDeatilAnalytics();


    }
    // 测试用例将写在这里
}