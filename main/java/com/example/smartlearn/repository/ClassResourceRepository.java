package com.example.smartlearn.repository;

import com.example.smartlearn.model.ClassResource;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ClassResourceRepository extends JpaRepository<ClassResource, Integer> {
    List<ClassResource> findByCourseId(Integer courseId);
    List<ClassResource> findByTaskId(Integer taskId);

    List<ClassResource> findByType(ClassResource.ResourceType type);
    List<ClassResource> findByNameContaining(String keyword);
    @Query("SELECT r FROM ClassResource r WHERE r.name = :name AND r.courseId = :courseId")
    Optional<ClassResource> findByCourseAndName(@Param("courseId") Integer courseId,
                                           @Param("name") String name);


    @Query("SELECT r FROM ClassResource r WHERE r.courseId = :courseId AND r.type = :type")

    List<ClassResource> findByCourseIdAndType(@Param("courseId") Integer courseId,
                                         @Param("type") ClassResource.ResourceType type);

    @Query("SELECT r FROM ClassResource r WHERE r.courseId = :courseId AND r.type = :type AND r.name LIKE %:keyword%")
    List<ClassResource> findByCourseIdAndTypeAndNameContaining(@Param("courseId") Integer courseId,
                                                          @Param("type") ClassResource.ResourceType type,
                                                          @Param("keyword") String keyword);

    @Query("SELECT r FROM ClassResource r WHERE r.type = :type AND r.courseId = :courseId")
    List<ClassResource> findByTypeAndCourseId(@Param("type") ClassResource.ResourceType type,
                                         @Param("courseId") Integer courseId);


    @Query("SELECT r FROM ClassResource r WHERE " +
            "(:courseIds IS NULL OR r.courseId IN :courseIds) AND " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:keyword IS NULL OR r.name LIKE %:keyword%)")
    List<ClassResource> findByCourseIdsAndTypeAndKeyword(
            @Param("courseIds") List<Integer> courseIds,
            @Param("type") ClassResource.ResourceType type,
            @Param("keyword") String keyword
    );
    Page<ClassResource> findByCourseIdIn(List<Long> courseIds, Pageable pageable);




    @Query("SELECT r.courseId FROM ClassResource r WHERE r.resourceId = :resourceId")
    Integer findCourseIdByResourceId(@Param("resourceId") Integer resourceId);

    // 添加新方法：仅更新URL
    @Modifying
    @Transactional
    @Query("UPDATE ClassResource r SET r.url = :url WHERE r.resourceId = :resourceId")
    void updateUrl(@Param("resourceId") Integer resourceId, @Param("url") String url);


    @Lock(LockModeType.PESSIMISTIC_WRITE) // 添加悲观锁
    @Query("SELECT r FROM ClassResource r WHERE r.resourceId = :resourceId")
    Optional<ClassResource> findByIdForUpdate(@Param("resourceId") Integer resourceId);

    @Query("SELECT r.courseId FROM ClassResource r WHERE r.resourceId = :resourceId")
    Integer getCourseIdByResourceId(@Param("resourceId") Integer resourceId); // 简化方法名

    @Query("SELECT r FROM ClassResource r WHERE r.resourceId = :resourceId")
    Optional<ClassResource> findById(@Param("resourceId") Integer resourceId);

    // ResourceRepository.java
    @Modifying
    @Transactional
    @Query("UPDATE ClassResource r SET r.url = :url WHERE r.resourceId = :resourceId")
    void updateResourceUrl(@Param("resourceId") Integer resourceId, @Param("url") String url);

    @Query("SELECT r FROM ClassResource r WHERE r.resourceId = :resourceId")
    Optional<ClassResource> findById(@Param("resourceId") Long resourceId);

    @Query("SELECT r FROM ClassResource r JOIN Course c ON r.courseId = c.courseId " +
            "WHERE r.courseId = :courseId AND c.teacherId = :teacherId")
    List<ClassResource> findByCourseIdAndTeacherId(
            @Param("courseId") Long courseId,
            @Param("teacherId") Long teacherId
    );
    @Query("SELECT cr FROM ClassResource cr " +
            "JOIN Course c ON  cr.courseId = c.courseId " +
            "WHERE c.teacherId = :teacherId")
    List<ClassResource> findByTeacherId(@Param("teacherId") Long teacherId);

    boolean existsByResourceId(Long resourceId);

    // 直接统计课程资源数量
    @Query("SELECT COUNT(cr) FROM ClassResource cr WHERE cr.courseId = :courseId")
    int countByCourseId(@Param("courseId") Long courseId);

    // 批量统计直接资源数量（按课程分组）
    @Query("SELECT cr.courseId, COUNT(cr) FROM ClassResource cr WHERE cr.courseId IN :courseIds GROUP BY cr.courseId")
    Map<Long, Integer> countByCourseIds(@Param("courseIds") List<Long> courseIds);

    // 查询直接关联课程的所有资源ID
    @Query("SELECT cr.resourceId FROM ClassResource cr WHERE cr.courseId = :courseId")
    Set<Long> findResourceIdsByCourseId(@Param("courseId") Long courseId);

}
