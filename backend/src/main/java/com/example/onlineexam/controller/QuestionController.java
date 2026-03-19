package com.example.onlineexam.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.example.onlineexam.annotation.LogAction;
import com.example.onlineexam.model.Question;
import com.example.onlineexam.payload.request.QuestionExcelDTO;
import com.example.onlineexam.repository.QuestionCategoryRepository;
import com.example.onlineexam.repository.QuestionRepository;
import com.example.onlineexam.service.QuestionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private QuestionCategoryRepository questionCategoryRepository;

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

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public Question createQuestion(@RequestBody Question question) {
        return questionService.createQuestion(question);
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
            @RequestParam("categoryId") Long categoryId
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "上传文件不能为空"));
        }
        if (!questionCategoryRepository.existsById(categoryId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "分类不存在: " + categoryId));
        }

        List<Question> toSave = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), QuestionExcelDTO.class, new PageReadListener<QuestionExcelDTO>(dataList -> {
                for (QuestionExcelDTO row : dataList) {
                    if (row == null || isBlank(row.getContent()) || isBlank(row.getAnswer())) {
                        continue;
                    }
                    Question question = new Question();
                    question.setCategoryId(categoryId);
                    question.setContent(row.getContent().trim());
                    question.setType(resolveType(row.getAnswer()));
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
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question questionDetails) {
        Question updatedQuestion = questionService.updateQuestion(id, questionDetails);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
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

    private String resolveType(String answer) {
        if (answer == null) return "SINGLE_CHOICE";
        String normalized = answer.trim().toUpperCase();
        if (normalized.contains(",")) return "MULTIPLE_CHOICE";
        if ("T".equals(normalized) || "F".equals(normalized) || "TRUE".equals(normalized) || "FALSE".equals(normalized)) {
            return "TRUE_FALSE";
        }
        return "SINGLE_CHOICE";
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
