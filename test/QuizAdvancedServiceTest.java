package com.example.smartlearn.service.teacher;

import com.example.smartlearn.dto.request.QuizAdvancedRequest;
import com.example.smartlearn.dto.response.QuizResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuizAdvancedServiceTest {
    private QuizAdvancedService quizAdvancedService;

    @BeforeEach
    void setUp() {
        quizAdvancedService = new QuizAdvancedService();
    }

    @Test
    void replaceQuestion_doesNotThrow() {
        QuizAdvancedRequest.ReplaceQuestionRequest req = new QuizAdvancedRequest.ReplaceQuestionRequest();
        assertDoesNotThrow(() -> quizAdvancedService.replaceQuestion(req));
    }

    @Test
    void reorderQuestions_doesNotThrow() {
        QuizAdvancedRequest.ReorderQuestionsRequest req = new QuizAdvancedRequest.ReorderQuestionsRequest();
        assertDoesNotThrow(() -> quizAdvancedService.reorderQuestions(req));
    }

    @Test
    void autoGenerateQuiz_returnsNull() {
        QuizAdvancedRequest.AutoGenerateQuizRequest req = new QuizAdvancedRequest.AutoGenerateQuizRequest();
        QuizResponse resp = quizAdvancedService.autoGenerateQuiz(req);
        assertNull(resp);
    }
} 