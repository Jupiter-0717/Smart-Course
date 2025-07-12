package com.example.smartlearn.service.teacher;

import com.example.smartlearn.controller.teacher.CourseStats;
import com.example.smartlearn.repository.KnowledgePointResourcesRepository;
import com.example.smartlearn.repository.KnowledgePointsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private KnowledgePointsRepository knowledgePointsRepo;

    @Mock
    private KnowledgePointResourcesRepository pointResourcesRepo;

    @InjectMocks
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        // 基础设置，如果需要的话
    }

    @Test
    void testGetCourseStats_success() {
        // 准备测试数据
        Long courseId = 1L;
        int expectedPointCount = 5;
        int expectedResourceCount = 10;

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseId(courseId)).thenReturn(expectedPointCount);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(courseId)).thenReturn(expectedResourceCount);

        // 执行测试
        CourseStats result = statsService.getCourseStats(courseId);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedPointCount, result.pointCount());
        assertEquals(expectedResourceCount, result.resourceCount());

        // 验证Repository方法被正确调用
        verify(knowledgePointsRepo).countByCourseId(courseId);
        verify(pointResourcesRepo).countDistinctResourcesByCourseId(courseId);
    }

    @Test
    void testGetCourseStats_zeroCounts() {
        // 准备测试数据
        Long courseId = 2L;
        int expectedPointCount = 0;
        int expectedResourceCount = 0;

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseId(courseId)).thenReturn(expectedPointCount);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(courseId)).thenReturn(expectedResourceCount);

        // 执行测试
        CourseStats result = statsService.getCourseStats(courseId);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.pointCount());
        assertEquals(0, result.resourceCount());
    }

    @Test
    void testGetCourseStats_largeNumbers() {
        // 准备测试数据
        Long courseId = 3L;
        int expectedPointCount = 1000;
        int expectedResourceCount = 5000;

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseId(courseId)).thenReturn(expectedPointCount);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(courseId)).thenReturn(expectedResourceCount);

        // 执行测试
        CourseStats result = statsService.getCourseStats(courseId);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedPointCount, result.pointCount());
        assertEquals(expectedResourceCount, result.resourceCount());
    }

    @Test
    void testGetBatchCourseStats_success() {
        // 准备测试数据
        List<Long> courseIds = Arrays.asList(1L, 2L, 3L);
        Map<Long, Integer> pointCounts = new HashMap<>();
        pointCounts.put(1L, 5);
        pointCounts.put(2L, 10);
        pointCounts.put(3L, 15);

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseIds(courseIds)).thenReturn(pointCounts);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(1L)).thenReturn(20);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(2L)).thenReturn(30);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(3L)).thenReturn(40);

        // 执行测试
        Map<Long, CourseStats> result = statsService.getBatchCourseStats(courseIds);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证每个课程的统计结果
        CourseStats stats1 = result.get(1L);
        assertNotNull(stats1);
        assertEquals(5, stats1.pointCount());
        assertEquals(20, stats1.resourceCount());

        CourseStats stats2 = result.get(2L);
        assertNotNull(stats2);
        assertEquals(10, stats2.pointCount());
        assertEquals(30, stats2.resourceCount());

        CourseStats stats3 = result.get(3L);
        assertNotNull(stats3);
        assertEquals(15, stats3.pointCount());
        assertEquals(40, stats3.resourceCount());

        // 验证Repository方法被正确调用
        verify(knowledgePointsRepo).countByCourseIds(courseIds);
        verify(pointResourcesRepo, times(3)).countDistinctResourcesByCourseId(anyLong());
    }

    @Test
    void testGetBatchCourseStats_emptyList() {
        // 准备测试数据
        List<Long> courseIds = Arrays.asList();
        Map<Long, Integer> pointCounts = new HashMap<>();

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseIds(courseIds)).thenReturn(pointCounts);

        // 执行测试
        Map<Long, CourseStats> result = statsService.getBatchCourseStats(courseIds);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());

        // 验证Repository方法被正确调用
        verify(knowledgePointsRepo).countByCourseIds(courseIds);
        verify(pointResourcesRepo, never()).countDistinctResourcesByCourseId(anyLong());
    }

    @Test
    void testGetBatchCourseStats_singleCourse() {
        // 准备测试数据
        List<Long> courseIds = Arrays.asList(1L);
        Map<Long, Integer> pointCounts = new HashMap<>();
        pointCounts.put(1L, 8);

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseIds(courseIds)).thenReturn(pointCounts);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(1L)).thenReturn(25);

        // 执行测试
        Map<Long, CourseStats> result = statsService.getBatchCourseStats(courseIds);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        CourseStats stats = result.get(1L);
        assertNotNull(stats);
        assertEquals(8, stats.pointCount());
        assertEquals(25, stats.resourceCount());
    }

    @Test
    void testGetBatchCourseStats_missingPointCounts() {
        // 准备测试数据 - 模拟某些课程没有知识点统计
        List<Long> courseIds = Arrays.asList(1L, 2L, 3L);
        Map<Long, Integer> pointCounts = new HashMap<>();
        pointCounts.put(1L, 5);
        // 2L 和 3L 没有在pointCounts中

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseIds(courseIds)).thenReturn(pointCounts);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(1L)).thenReturn(20);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(2L)).thenReturn(30);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(3L)).thenReturn(40);

        // 执行测试
        Map<Long, CourseStats> result = statsService.getBatchCourseStats(courseIds);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证有统计数据的课程
        CourseStats stats1 = result.get(1L);
        assertEquals(5, stats1.pointCount());
        assertEquals(20, stats1.resourceCount());

        // 验证没有统计数据的课程应该返回0
        CourseStats stats2 = result.get(2L);
        assertEquals(0, stats2.pointCount());
        assertEquals(30, stats2.resourceCount());

        CourseStats stats3 = result.get(3L);
        assertEquals(0, stats3.pointCount());
        assertEquals(40, stats3.resourceCount());
    }

    @Test
    void testGetBatchCourseStats_mixedData() {
        // 准备测试数据 - 模拟部分课程有数据，部分课程没有数据
        List<Long> courseIds = Arrays.asList(1L, 2L, 3L);
        Map<Long, Integer> pointCounts = new HashMap<>();
        pointCounts.put(1L, 5);
        pointCounts.put(3L, 15);
        // 2L 没有在pointCounts中，应该使用默认值0

        // Mock Repository方法
        when(knowledgePointsRepo.countByCourseIds(courseIds)).thenReturn(pointCounts);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(1L)).thenReturn(20);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(2L)).thenReturn(30);
        when(pointResourcesRepo.countDistinctResourcesByCourseId(3L)).thenReturn(40);

        // 执行测试
        Map<Long, CourseStats> result = statsService.getBatchCourseStats(courseIds);

        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证有统计数据的课程
        CourseStats stats1 = result.get(1L);
        assertEquals(5, stats1.pointCount());
        assertEquals(20, stats1.resourceCount());

        // 验证没有统计数据的课程应该返回0
        CourseStats stats2 = result.get(2L);
        assertEquals(0, stats2.pointCount());
        assertEquals(30, stats2.resourceCount());

        CourseStats stats3 = result.get(3L);
        assertEquals(15, stats3.pointCount());
        assertEquals(40, stats3.resourceCount());
    }
} 