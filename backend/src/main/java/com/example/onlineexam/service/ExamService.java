package com.example.onlineexam.service;

import com.example.onlineexam.model.Course;
import com.example.onlineexam.model.Exam;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.repository.ExamRepository;
import com.example.onlineexam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuestionRepository questionRepository;

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

        // List<Question> questions = questionRepository.findByCourseId(courseId);
        // Collections.shuffle(questions);
        // List<Question> selectedQuestions = questions.subList(0, Math.min(numberOfQuestions, questions.size()));
        // exam.setQuestions(selectedQuestions);
        exam.setQuestions(Collections.emptyList());

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

    public void deleteExam(Long id) {
        examRepository.deleteById(id);
    }
}
