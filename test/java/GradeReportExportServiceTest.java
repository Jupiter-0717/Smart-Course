package com.example.smartlearn.service.teacher;

import com.example.smartlearn.dto.response.CourseGradeReportResponse;
import com.example.smartlearn.dto.response.CourseGradeReportResponse.CourseStatistics;
import com.example.smartlearn.dto.response.CourseGradeReportResponse.StudentGradeInfo;
import com.example.smartlearn.service.teacher.GradeAnalysisService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

class GradeReportExportServiceTest {

    @Mock
    private GradeAnalysisService gradeAnalysisService;

    @InjectMocks
    private GradeReportExportService exportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 正常导出：有学生成绩、有统计信息
     */
    @Test
    void testExportCourseGradeReport_NormalCase() throws Exception {
        CourseGradeReportResponse mockReport = buildMockReportWithOneStudent();
        when(gradeAnalysisService.getCourseGradeReport(1L)).thenReturn(mockReport);

        byte[] excelBytes = exportService.exportCourseGradeReport(1L);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("成绩报表");
            assertNotNull(sheet);

            Row titleRow = sheet.getRow(0);
            assertEquals("课程成绩报表", titleRow.getCell(0).getStringCellValue());

            Row studentRow = sheet.getRow(2);
            assertEquals("20230101", studentRow.getCell(0).getStringCellValue());
            assertEquals("张三", studentRow.getCell(1).getStringCellValue());
        }
    }



    /**
     * 异常情况：GradeAnalysisService 抛出异常
     */
    @Test
    void testExportCourseGradeReport_ServiceThrows() {
        when(gradeAnalysisService.getCourseGradeReport(999L))
                .thenThrow(new RuntimeException("数据库连接失败"));

        IOException exception = assertThrows(IOException.class, () ->
                exportService.exportCourseGradeReport(999L));

        assertTrue(exception.getMessage().contains("导出失败"));
    }



    // 构造带一个学生的报表
    private CourseGradeReportResponse buildMockReportWithOneStudent() {
        StudentGradeInfo student = new StudentGradeInfo();
        student.setStudentId(1L);
        student.setStudentName("张三");
        student.setStudentNumber("20230101");
        student.setTotalGrade(BigDecimal.valueOf(90));
        student.setMaxGrade(BigDecimal.valueOf(100));
        student.setGradePercentage(0.9);
        student.setGradeLevel("A");
        student.setRank(1);
        student.setCompletedTasks(5);
        student.setTotalTasks(5);
        student.setCompletionRate(1.0);

        CourseStatistics stats = new CourseStatistics();
        stats.setTotalStudents(1);
        stats.setClassAverage(BigDecimal.valueOf(90));
        stats.setHighestGrade(BigDecimal.valueOf(90));
        stats.setLowestGrade(BigDecimal.valueOf(90));
        stats.setStandardDeviation(BigDecimal.ZERO);
        stats.setGradeDistribution(new HashMap<>(Map.of("A", 1)));

        CourseGradeReportResponse report = new CourseGradeReportResponse();
        report.setCourseName("Java课程");
        report.setStudentGrades(List.of(student));
        report.setCourseStatistics(stats);
        return report;
    }
}
