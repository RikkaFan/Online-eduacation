package com.example.onlineexam.service;

import com.example.onlineexam.model.Course;
import com.example.onlineexam.model.Exam;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.repository.ExamRepository;
import com.example.onlineexam.repository.ExamResultRepository;
import com.example.onlineexam.repository.QuestionRepository;
import com.example.onlineexam.repository.StudentAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    public List<Exam> getExamsByCourse(Long courseId) {
        return examRepository.findByCourse_Id(courseId);
    }

    public Optional<Exam> getExamById(Long id) {
        return examRepository.findById(id);
    }

    public Exam createExam(Long courseId, Exam exam, int numberOfQuestions, List<Long> questionIds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        exam.setCourse(course);

        List<Question> selectedQuestions = new ArrayList<>();
        if (questionIds != null && !questionIds.isEmpty()) {
            selectedQuestions = questionRepository.findAllById(questionIds);
        } else if (exam.getQuestions() != null && !exam.getQuestions().isEmpty()) {
            Set<Long> ids = exam.getQuestions().stream()
                    .map(Question::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                selectedQuestions = questionRepository.findAllById(ids);
            }
        } else if (hasTypedRule(exam)) {
            List<Question> allQuestions = questionRepository.findByCourseId(courseId);
            List<Question> singleQuestions = allQuestions.stream().filter(q -> "SINGLE".equals(normalizeType(q.getType()))).collect(Collectors.toList());
            List<Question> multipleQuestions = allQuestions.stream().filter(q -> "MULTIPLE".equals(normalizeType(q.getType()))).collect(Collectors.toList());
            List<Question> judgeQuestions = allQuestions.stream().filter(q -> "JUDGE".equals(normalizeType(q.getType()))).collect(Collectors.toList());
            List<Question> subjectiveQuestions = allQuestions.stream().filter(q -> "SUBJECTIVE".equals(normalizeType(q.getType()))).collect(Collectors.toList());

            selectedQuestions.addAll(pickQuestions(singleQuestions, valueOrZero(exam.getSingleCount()), "单选题"));
            selectedQuestions.addAll(pickQuestions(multipleQuestions, valueOrZero(exam.getMultipleCount()), "多选题"));
            selectedQuestions.addAll(pickQuestions(judgeQuestions, valueOrZero(exam.getJudgeCount()), "判断题"));
            selectedQuestions.addAll(pickQuestions(subjectiveQuestions, valueOrZero(exam.getSubjectiveCount()), "主观题"));
        } else if (numberOfQuestions > 0) {
            List<Question> allQuestions = questionRepository.findByCourseId(courseId);
            Collections.shuffle(allQuestions);
            selectedQuestions = allQuestions.subList(0, Math.min(numberOfQuestions, allQuestions.size()));
        }
        exam.setQuestions(selectedQuestions);
        exam.setTotalScore(calculateTotalScore(exam, selectedQuestions));

        return examRepository.save(exam);
    }

    public Exam updateExam(Long id, Exam examDetails) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));

        exam.setTitle(examDetails.getTitle());
        exam.setStartTime(examDetails.getStartTime());
        exam.setEndTime(examDetails.getEndTime());
        exam.setSingleCount(examDetails.getSingleCount());
        exam.setSingleScore(examDetails.getSingleScore());
        exam.setMultipleCount(examDetails.getMultipleCount());
        exam.setMultipleScore(examDetails.getMultipleScore());
        exam.setJudgeCount(examDetails.getJudgeCount());
        exam.setJudgeScore(examDetails.getJudgeScore());
        exam.setSubjectiveCount(examDetails.getSubjectiveCount());
        exam.setSubjectiveScore(examDetails.getSubjectiveScore());
        exam.setTotalScore(examDetails.getTotalScore());

        return examRepository.save(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        examResultRepository.deleteByExamId(id);
        studentAnswerRepository.deleteByExam_Id(id);
        examRepository.deleteExamQuestionsByExamId(id);
        examRepository.deleteById(id);
    }

    private List<Question> pickQuestions(List<Question> source, int required, String typeName) {
        if (required <= 0) return List.of();
        if (source.size() < required) {
            throw new RuntimeException("题库中" + typeName + "数量不足");
        }
        Collections.shuffle(source);
        return new ArrayList<>(source.subList(0, required));
    }

    private boolean hasTypedRule(Exam exam) {
        return valueOrZero(exam.getSingleCount()) > 0
                || valueOrZero(exam.getMultipleCount()) > 0
                || valueOrZero(exam.getJudgeCount()) > 0
                || valueOrZero(exam.getSubjectiveCount()) > 0;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private int scoreByType(Exam exam, String type) {
        return switch (normalizeType(type)) {
            case "MULTIPLE" -> Math.max(valueOrZero(exam.getMultipleScore()), 1);
            case "JUDGE" -> Math.max(valueOrZero(exam.getJudgeScore()), 1);
            case "SUBJECTIVE" -> Math.max(valueOrZero(exam.getSubjectiveScore()), 1);
            default -> Math.max(valueOrZero(exam.getSingleScore()), 1);
        };
    }

    private int calculateTotalScore(Exam exam, List<Question> questions) {
        int total = 0;
        for (Question question : questions) {
            total += scoreByType(exam, question.getType());
        }
        return total;
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) return "SINGLE";
        String normalized = type.trim().toUpperCase();
        return switch (normalized) {
            case "MULTIPLE", "MULTIPLE_CHOICE" -> "MULTIPLE";
            case "JUDGE", "TRUE_FALSE" -> "JUDGE";
            case "SUBJECTIVE" -> "SUBJECTIVE";
            default -> "SINGLE";
        };
    }
}
