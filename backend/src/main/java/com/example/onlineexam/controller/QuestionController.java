package com.example.onlineexam.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.example.onlineexam.annotation.LogAction;
import com.example.onlineexam.model.Course;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.payload.request.QuestionExcelDTO;
import com.example.onlineexam.repository.CourseRepository;
import com.example.onlineexam.repository.QuestionRepository;
import com.example.onlineexam.service.QuestionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public List<Question> getAllQuestions() {
        return questionService.getAllQuestions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return ResponseEntity.ok(question);
    }

    @GetMapping("/category/{categoryId}")
    public List<Question> getQuestionsByCategory(@PathVariable Long categoryId) {
        return questionService.getQuestionsByCategoryId(categoryId);
    }

    @GetMapping("/course/{courseId}")
    public List<Question> getQuestionsByCourse(@PathVariable Long courseId) {
        return questionService.getQuestionsByCourseId(courseId);
    }

    @GetMapping("/practice/generate")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Question> generatePractice(@RequestParam Long courseId, @RequestParam(defaultValue = "10") int count) {
        return questionService.generatePracticeQuestions(courseId, count);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("新增了题目")
    public ResponseEntity<?> createQuestion(@RequestBody Question question) {
        if (question == null || isBlank(question.getContent()) || isBlank(question.getAnswer())) {
            return ResponseEntity.badRequest().body(Map.of("message", "题干和答案不能为空"));
        }
        if (question.getCategoryId() == null && question.getCourseId() != null) {
            question.setCategoryId(question.getCourseId());
        }
        if (question.getCourseId() == null && question.getCategoryId() != null) {
            question.setCourseId(question.getCategoryId());
        }
        if (question.getCourseId() == null && question.getCategoryId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "courseId 与 categoryId 不能同时为空"));
        }
        try {
            return ResponseEntity.ok(questionService.createQuestion(question));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "题目数据不符合存储要求，请检查题干、答案和选项长度"));
        }
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("Question_Template", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName + ".xlsx");

        QuestionExcelDTO sample = new QuestionExcelDTO();
        sample.setContent("示例：以下哪项属于关系型数据库？");
        sample.setOptionA("MySQL");
        sample.setOptionB("Redis");
        sample.setOptionC("Kafka");
        sample.setOptionD("Nacos");
        sample.setType("SINGLE");
        sample.setAnswer("A");
        sample.setAnalysis("MySQL 是关系型数据库。");
        sample.setScore(5);

        EasyExcel.write(response.getOutputStream(), QuestionExcelDTO.class)
                .sheet("题库导入模板")
                .doWrite(List.of(sample));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("批量导入了题库")
    public ResponseEntity<?> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "上传文件不能为空"));
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在: " + courseId));

        List<Question> toSave = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), QuestionExcelDTO.class, new PageReadListener<QuestionExcelDTO>(dataList -> {
                for (QuestionExcelDTO row : dataList) {
                    if (row == null || isBlank(row.getContent()) || isBlank(row.getAnswer())) {
                        continue;
                    }
                    Question question = new Question();
                    question.setCategoryId(course.getId());
                    question.setCourseId(course.getId());
                    question.setContent(row.getContent().trim());
                    question.setType(resolveType(row.getType(), row.getAnswer()));
                    question.setAnswer(row.getAnswer().trim().toUpperCase());
                    question.setOptions(buildOptions(row));
                    toSave.add(question);
                }
            })).sheet().doRead();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "读取 Excel 失败: " + e.getMessage()));
        }

        if (toSave.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "未解析到有效题目数据"));
        }
        questionRepository.saveAll(toSave);
        return ResponseEntity.ok(Map.of("message", "导入成功", "count", toSave.size()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("更新了题目")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question questionDetails) {
        Question updatedQuestion = questionService.updateQuestion(id, questionDetails);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("删除了题目")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    private String buildOptions(QuestionExcelDTO row) {
        String a = safe(row.getOptionA());
        String b = safe(row.getOptionB());
        String c = safe(row.getOptionC());
        String d = safe(row.getOptionD());
        return String.format("A:%s, B:%s, C:%s, D:%s", a, b, c, d);
    }

    private String resolveType(String type, String answer) {
        if (type != null && !type.trim().isEmpty()) {
            String normalizedType = type.trim().toUpperCase();
            if ("MULTIPLE".equals(normalizedType) || "MULTIPLE_CHOICE".equals(normalizedType)) {
                return "MULTIPLE";
            }
            if ("JUDGE".equals(normalizedType) || "TRUE_FALSE".equals(normalizedType)) {
                return "JUDGE";
            }
            if ("SUBJECTIVE".equals(normalizedType)) {
                return "SUBJECTIVE";
            }
            return "SINGLE";
        }
        if (answer != null) {
            String normalizedAnswer = answer.trim().toUpperCase();
            if ("T".equals(normalizedAnswer) || "F".equals(normalizedAnswer) || "TRUE".equals(normalizedAnswer) || "FALSE".equals(normalizedAnswer)) {
                return "JUDGE";
            }
        }
        if (answer != null && answer.trim().contains(",")) return "MULTIPLE";
        return "SINGLE";
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
