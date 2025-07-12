// KnowledgeGraphController.java
package com.example.smartlearn.controller.teacher;

import com.example.smartlearn.dto.KnowledgeGraphDTO;
import com.example.smartlearn.dto.KnowledgePointDTO;
import com.example.smartlearn.dto.PositionUpdateDTO;
import com.example.smartlearn.service.teacher.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers/{teacherId}/knowledge-graph")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    // 创建知识点
    @PostMapping("/points")
    public ResponseEntity<Long> createKnowledgePoint(
            @PathVariable Long teacherId,
            @RequestBody KnowledgePointDTO dto) {
        Long pointId = knowledgeGraphService.createKnowledgePoint(dto);
        return ResponseEntity.ok(pointId);
    }

    // 获取课程知识图谱
    @GetMapping("/course/{courseId}")
    public ResponseEntity<KnowledgeGraphDTO> getCourseKnowledgeGraph(
            @PathVariable Long teacherId,
            @PathVariable Long courseId) {

        KnowledgeGraphDTO graph = knowledgeGraphService.getCourseKnowledgeGraph(courseId);
        return ResponseEntity.ok(graph);
    }


    // 更新知识点位置
    @PutMapping("/points/{pointId}/position")
    public ResponseEntity<Void> updatePosition(
            @PathVariable Long teacherId,
            @PathVariable Long pointId,
            @RequestBody PositionUpdateDTO position) {
        knowledgeGraphService.updatePosition(pointId, position);
        return ResponseEntity.ok().build();
    }

    // 删除知识点
    @DeleteMapping("/points/{pointId}")
    public ResponseEntity<Void> deleteKnowledgePoint(
            @PathVariable Long teacherId,
            @PathVariable Long pointId) {
        knowledgeGraphService.deleteKnowledgePoint(pointId);
        return ResponseEntity.ok().build();
    }

    // AI生成知识图谱
    @PostMapping("/{courseId}/generate-by-ai")
    public ResponseEntity<KnowledgeGraphDTO> generateByAI(
            @PathVariable Long teacherId,
            @PathVariable Long courseId) {
        KnowledgeGraphDTO graph = knowledgeGraphService.generateByAI(courseId);
        System.out.println(graph);
        return ResponseEntity.ok(graph);
    }
}