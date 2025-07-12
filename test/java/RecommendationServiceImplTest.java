package com.example.smartlearn;

import com.example.smartlearn.model.*;
import com.example.smartlearn.repository.*;
import com.example.smartlearn.service.student.RecommendationServiceImpl;
import com.example.smartlearn.service.student.WeightedResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceImplTest {

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private KnowledgePointResourceRepository knowledgePointResourceRepository;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Test
    public void whenGetWeightedRecommendations_thenReturnSortedResources() {
        // 准备测试数据
        Long studentId = 1L;

        // 模拟错题数据
        Question question1 = new Question();
        question1.setId(1L);
        question1.setDifficulty(3); // 难度3
        KnowledgePoint kp1 = new KnowledgePoint();
        kp1.setId(101L);
        question1.setKnowledgePoint(kp1);

        Question question2 = new Question();
        question2.setId(2L);
        question2.setDifficulty(5); // 难度5
        KnowledgePoint kp2 = new KnowledgePoint();
        kp2.setId(102L);
        question2.setKnowledgePoint(kp2);

        Submission submission = new Submission();
        Task quizTask = new Task();
        quizTask.setType(Task.TaskType.QUIZ);
        quizTask.setId(10L);
        submission.setTask(quizTask);

        StudentAnswer answer1 = new StudentAnswer();
        answer1.setQuestion(question1);
        answer1.setScore(BigDecimal.valueOf(2)); // 得分2/5
        answer1.setSubmission(submission);

        StudentAnswer answer2 = new StudentAnswer();
        answer2.setQuestion(question2);
        answer2.setScore(BigDecimal.valueOf(1)); // 得分1/5
        answer2.setSubmission(submission);

        List<StudentAnswer> wrongAnswers = Arrays.asList(answer1, answer2);

        // 模拟知识点资源
        ClassResource resource1 = new ClassResource();
        resource1.setId(1001L);
        resource1.setType(ClassResource.ResourceType.ppt);

        ClassResource resource2 = new ClassResource();
        resource2.setId(1002L);
        resource2.setType(ClassResource.ResourceType.pdf);

        KnowledgePointResource kpr1 = new KnowledgePointResource();
        kpr1.setKnowledgePoint(kp1);
        kpr1.setResource(resource1);

        KnowledgePointResource kpr2 = new KnowledgePointResource();
        kpr2.setKnowledgePoint(kp2);
        kpr2.setResource(resource2);

        // 模拟repository行为
        when(studentAnswerRepository.findWrongAnswersWithQuestions(studentId)).thenReturn(wrongAnswers);
        when(quizQuestionRepository.findScoreByQuizIdAndQuestionId(10L, 1L)).thenReturn(5);
        when(quizQuestionRepository.findScoreByQuizIdAndQuestionId(10L, 2L)).thenReturn(5);
        when(knowledgePointResourceRepository.findWithResourcesByKnowledgePointIdIn(anyList()))
                .thenReturn(Arrays.asList(kpr1, kpr2));
        List<WeightedResource> result = recommendationService.getWeightedRecommendations(studentId);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getWeight() > result.get(1).getWeight());
        assertEquals(ClassResource.ResourceType.pdf, result.get(0).getResource().getType());
        verify(studentAnswerRepository).findWrongAnswersWithQuestions(studentId);
    }

}