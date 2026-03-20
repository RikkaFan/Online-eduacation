package com.example.onlineexam.payload.response;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreExcelDTO {
    @ExcelProperty("学生姓名")
    private String studentName;

    @ExcelProperty("考试名称")
    private String examTitle;

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("得分")
    private Double score;

    @ExcelProperty("交卷时间")
    private String submitTime;
}
