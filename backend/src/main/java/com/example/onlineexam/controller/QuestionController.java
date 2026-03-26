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
import com.example.onlineexam.service.QuestionTextImportService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import com.example.onlineexam.security.UserDetailsImpl;

import java.io.IOException;
import java.io.ByteArrayInputStream;
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

    @Autowired
    private QuestionTextImportService questionTextImportService;

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
        enforceTeacherCourseOwnership(course);

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

    @PostMapping(value = "/import/quick", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @LogAction("一键导入了题库")
    public ResponseEntity<?> quickImportQuestions(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在: " + courseId));
        enforceTeacherCourseOwnership(course);

        String sourceText = text;
        if ((sourceText == null || sourceText.trim().isEmpty()) && file != null && !file.isEmpty()) {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            if (!filename.endsWith(".docx") && !filename.endsWith(".txt")) {
                return ResponseEntity.badRequest().body(Map.of("message", "仅支持 .docx 或 .txt 文件"));
            }
            try {
                if (filename.endsWith(".docx")) {
                    sourceText = extractDocxText(file);
                } else {
                    sourceText = new String(file.getBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "解析文件失败，请确认是可读取的 docx/txt 文件: " + e.getMessage()));
            }
        }

        QuestionTextImportService.ParseResult parseResult = questionTextImportService.parse(sourceText, courseId);
        List<Question> parsed = parseResult.getQuestions();
        List<String> errors = new ArrayList<>(parseResult.getErrors());
        if (parsed.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "未解析到可导入题目",
                    "successCount", 0,
                    "failedCount", errors.size(),
                    "errors", errors
            ));
        }

        int successCount = 0;
        for (Question question : parsed) {
            try {
                questionService.createQuestion(question);
                successCount++;
            } catch (Exception e) {
                errors.add("题目《" + safe(question.getContent()) + "》导入失败: " + e.getMessage());
            }
        }

        int failedCount = parsed.size() - successCount + parseResult.getErrors().size();
        return ResponseEntity.ok(Map.of(
                "message", successCount > 0 ? "导入完成" : "导入失败",
                "successCount", successCount,
                "failedCount", failedCount,
                "errors", errors
        ));
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

    private void enforceTeacherCourseOwnership(Course course) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl user)) {
            return;
        }
        boolean teacher = auth.getAuthorities().stream().anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
        if (teacher) {
            Long ownerId = course == null ? null : course.getTeacherId();
            if (ownerId == null || !ownerId.equals(user.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("无权限导入到其他教师课程");
            }
        }
    }

    private String extractDocxText(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return String.join("\n", document.getParagraphs().stream()
                    .map(p -> p == null ? "" : p.getText())
                    .toList());
        } catch (Exception ignored) {
            try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
                 WordExtractor extractor = new WordExtractor(document)) {
                return extractor.getText();
            }
        }
    }
}
