package com.example.smartlearn.service.teacher;

import com.example.smartlearn.dto.response.Task_ResourceResponse;
import com.example.smartlearn.exception.ResourceNotFoundException;
import com.example.smartlearn.model.Course;
import com.example.smartlearn.model.Task;
import com.example.smartlearn.model.Task_Resource;
import com.example.smartlearn.model.Teacher;
import com.example.smartlearn.repository.TaskRepository;
import com.example.smartlearn.repository.Task_ResourceRepository;
import com.example.smartlearn.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Task_ResourceServiceTest {

    @InjectMocks
    private Task_ResourceService taskResourceService;

    @Mock
    private Task_ResourceRepository taskResourceRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 反射设置 uploadBasePath 字段
        Field field = Task_ResourceService.class.getDeclaredField("uploadBasePath");
        field.setAccessible(true);
        field.set(taskResourceService, "test-uploads");
    }


    @Test
    void testUploadTaskResources_Success() {
        // 准备数据
        Long taskId = 1L;
        Long teacherId = 2L;
        Course course = new Course();
        Task task = new Task();
        //task.setTaskId(taskId);

        task.setCourse(course);
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherId);

        MultipartFile mockFile = new MockMultipartFile(
                "file", "demo.txt", "text/plain", "hello".getBytes()
        );

        // 模拟依赖行为
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(taskResourceRepository.save(any())).thenAnswer(invocation -> {
            Task_Resource r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        // 执行方法
        List<Task_Resource> result = taskResourceService.uploadTaskResources(taskId, List.of(mockFile), teacherId);

        // 验证结果
        assertEquals(1, result.size());
        Task_Resource saved = result.get(0);
        assertEquals("demo.txt", saved.getName());
        assertEquals("text/plain", saved.getFileType());
        assertTrue(saved.getFilePath().contains("/uploads/"));
        assertEquals(task, saved.getTask());
        assertEquals(teacher, saved.getUploader());
    }

    @Test
    void testUploadTaskResources_TaskNotFound() {
        Long taskId = 1L;
        Long teacherId = 2L;
        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        Exception e = assertThrows(ResourceNotFoundException.class, () ->
                taskResourceService.uploadTaskResources(taskId, List.of(file), teacherId));

        assertTrue(e.getMessage().contains("任务不存在"));
    }

    @Test
    void testUploadTaskResources_TeacherNotFound() {
        Long taskId = 1L;
        Long teacherId = 2L;
        Task task = new Task();
        task.setCourse(new Course());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.empty());

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        Exception e = assertThrows(ResourceNotFoundException.class, () ->
                taskResourceService.uploadTaskResources(taskId, List.of(file), teacherId));

        assertTrue(e.getMessage().contains("教师不存在"));
    }

    @Test
    void testGetResourcesByTaskId_Success() {
        Long taskId = 1L;

        Task_Resource res = new Task_Resource();
        res.setId(1L);
        res.setName("test.pdf");
        res.setFileType("application/pdf");
        res.setFilePath("http://localhost:8080/uploads/test.pdf");
        res.setUploadDate(LocalDateTime.now());

        Teacher uploader = new Teacher();
        uploader.setTeacherId(42L);
        res.setUploader(uploader);

        when(taskResourceRepository.findByTaskId(taskId)).thenReturn(List.of(res));

        List<Task_ResourceResponse> list = taskResourceService.getResourcesByTaskId(taskId);

        assertEquals(1, list.size());
        Task_ResourceResponse r = list.get(0);
        assertEquals("test.pdf", r.getName());
        assertEquals("application/pdf", r.getFileType());
        assertEquals("http://localhost:8080/uploads/test.pdf", r.getFilePath());
        assertEquals(42L, r.getUploaderId());
    }
}
