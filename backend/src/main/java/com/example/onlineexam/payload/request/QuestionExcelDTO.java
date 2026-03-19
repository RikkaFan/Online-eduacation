package com.example.onlineexam.payload.request;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionExcelDTO {
    @ExcelProperty("题干")
    private String content;

    @ExcelProperty("选项A")
    private String optionA;

    @ExcelProperty("选项B")
    private String optionB;

    @ExcelProperty("选项C")
    private String optionC;

    @ExcelProperty("选项D")
    private String optionD;

    @ExcelProperty("正确答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("分数")
    private Integer score = 5;
}
