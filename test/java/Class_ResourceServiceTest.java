package com.example.smartlearn;

import com.example.smartlearn.model.ClassResource;
import com.example.smartlearn.repository.ClassResourceRepository;
import com.example.smartlearn.repository.CourseRepository;
import com.example.smartlearn.service.teacher.Class_ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Class_ResourceServiceTest {

    @Mock
    private ClassResourceRepository resourceRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private Class_ResourceService resourceService;

    @Captor
    private ArgumentCaptor<ClassResource> resourceCaptor;

    private final String testUploadDir = "test-uploads";
    private final Long testCourseId = 1L; // 使用Long类型
    private final Long testResourceId = 1L; // 使用Long类型
    private ClassResource testResource;
    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(resourceService, "uploadDir", testUploadDir);
        ReflectionTestUtils.setField(resourceService, "maxFileSize", 104857600L); // 100MB

        // 创建测试资源 - 使用Long类型ID
        testResource = new ClassResource();
        testResource.setResourceId(testResourceId); // 使用Long
        testResource.setCourseId(testCourseId.intValue()); // 课程ID是int类型
        testResource.setName("Test Resource");
        testResource.setType(ClassResource.ResourceType.pdf); // 使用小写
        testResource.setUrl("/uploads/courses/1/resources/pdf/test.pdf");
        testResource.setDescription("Test Description");
        testResource.setCreatedAt(LocalDateTime.now());

        // 创建测试文件
        testFile = new MockMultipartFile(
                "test.pdf",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );
    }

    @Test
    void uploadResource_Success() throws IOException {
        // 准备测试数据
        String name = "New Resource";
        ClassResource.ResourceType type = ClassResource.ResourceType.pdf; // 使用小写
        String description = "New Description";

        // 模拟依赖行为
        when(resourceRepository.save(any(ClassResource.class))).thenReturn(testResource);

        // 执行测试
        ClassResource result = resourceService.uploadResource(
                testCourseId.intValue(), // 转换为int
                name,
                type,
                testFile,
                description
        );

        // 验证结果
        assertNotNull(result);
        assertEquals(testResourceId, result.getResourceId());

        // 验证资源保存
        verify(resourceRepository).save(resourceCaptor.capture());
        ClassResource savedResource = resourceCaptor.getValue();
        assertEquals(name, savedResource.getName());
        assertEquals(type, savedResource.getType());
        assertEquals(description, savedResource.getDescription());
        assertTrue(savedResource.getUrl().contains("/uploads/courses/1/resources/pdf/"));
    }


    @Test
    void updateResource_Success() {
        // 准备测试数据
        String newName = "Updated Resource";
        String newDescription = "Updated Description";

        // 模拟依赖行为
        when(resourceRepository.findById(testResourceId.intValue())).thenReturn(Optional.of(testResource));
        when(resourceRepository.save(any(ClassResource.class))).thenReturn(testResource);

        // 执行测试
        ClassResource result = resourceService.updateResource(testResourceId.intValue(), newName, newDescription); // 转换为int

        // 验证结果
        assertNotNull(result);
        assertEquals(newName, result.getName());
        assertEquals(newDescription, result.getDescription());
    }

    @Test
    void getCourseResourceLocalPath_Success() {
        // 准备测试数据
        List<ClassResource> resources = Arrays.asList(
                createResource(1L, ClassResource.ResourceType.ppt),
                createResource(2L, ClassResource.ResourceType.pdf),
                createResource(3L, ClassResource.ResourceType.doc)
        );

        // 模拟依赖行为
        when(resourceRepository.findByCourseId(testCourseId.intValue())).thenReturn(resources);

        // 执行测试
        List<String> paths = resourceService.getCourseResourceLocalPath(testCourseId);

        // 验证结果
        assertNotNull(paths);
        assertEquals(3, paths.size());
        assertTrue(paths.get(0).contains("ppt"));
        assertTrue(paths.get(1).contains("pdf"));
        assertTrue(paths.get(2).contains("doc"));
    }

    @Test
    void convertLocalPathToUrl_Success() {
        // 准备测试数据
        String localPath = testUploadDir + "/courses/1/resources/pdf/test.pdf";

        // 执行测试
        String url = resourceService.convertLocalPathToUrl(localPath);

        // 验证结果
        assertEquals("/uploads/courses/1/resources/pdf/test.pdf", url);
    }



    @Test
    void getCourseResource_Success() {
        // 准备测试数据
        List<ClassResource> resources = Collections.singletonList(testResource);

        // 模拟依赖行为
        when(resourceRepository.findByCourseIdAndType(
                testCourseId.intValue(), ClassResource.ResourceType.pdf
        )).thenReturn(resources);

        // 执行测试
        List<ClassResource> result = resourceService.getCourseResource(
                testCourseId, ClassResource.ResourceType.pdf
        );

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testResourceId, result.get(0).getResourceId());
    }

    // 辅助方法：创建测试资源 - 使用Long类型ID
    private ClassResource createResource(Long id, ClassResource.ResourceType type) {
        ClassResource resource = new ClassResource();
        resource.setResourceId(id); // 使用Long
        resource.setCourseId(testCourseId.intValue()); // 课程ID是int类型
        resource.setName("Resource " + id);
        resource.setType(type); // 使用小写枚举
        resource.setUrl("/uploads/courses/1/resources/" + type.name() + "/file" + id);
        return resource;
    }

    // 辅助方法：创建测试文件
    private Path createTestFile() throws IOException {
        Path filePath = Paths.get(testUploadDir, "courses", "1", "resources", "pdf", "test.pdf");
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "test content".getBytes());
        return filePath;
    }

    // 辅助类：模拟MultipartFile
    static class MockMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public MockMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content; }
        @Override public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
        @Override public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}