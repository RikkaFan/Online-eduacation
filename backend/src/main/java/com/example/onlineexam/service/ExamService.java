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

    public Exam createExam(Long courseId, Exam exam, int numberOfQuestions) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
        exam.setCourse(course);

        List<Question> selectedQuestions = Collections.emptyList();
        if (exam.getQuestions() != null && !exam.getQuestions().isEmpty()) {
            Set<Long> ids = exam.getQuestions().stream()
                    .map(Question::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                selectedQuestions = questionRepository.findAllById(ids);
            }
        } else if (numberOfQuestions > 0) {
            List<Question> allQuestions = questionRepository.findByCourseId(courseId);
            Collections.shuffle(allQuestions);
            selectedQuestions = allQuestions.subList(0, Math.min(numberOfQuestions, allQuestions.size()));
        }
        exam.setQuestions(selectedQuestions);

        return examRepository.save(exam);
    }

    public Exam updateExam(Long id, Exam examDetails) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));

        exam.setTitle(examDetails.getTitle());
        exam.setStartTime(examDetails.getStartTime());
        exam.setEndTime(examDetails.getEndTime());

        return examRepository.save(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        examResultRepository.deleteByExamId(id);
        studentAnswerRepository.deleteByExam_Id(id);
        examRepository.deleteExamQuestionsByExamId(id);
        examRepository.deleteById(id);
    }
}
