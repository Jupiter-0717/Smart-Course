package com.example.smartlearn.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "knowledge_points")
public class KnowledgePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 自引用的父知识点
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private KnowledgePoint parent;

    // 一个父知识点可以有多个子知识点
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<KnowledgePoint> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public KnowledgePoint getParent() {
        return parent;
    }

    public void setParent(KnowledgePoint parent) {
        this.parent = parent;
    }

    public List<KnowledgePoint> getChildren() {
        return children;
    }

    public void setChildren(List<KnowledgePoint> children) {
        this.children = children;
    }
}
