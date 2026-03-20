<template>
  <div class="student-smart-practice">
    <el-card class="glass-card top-panel" shadow="never">
      <div class="title">自主刷题</div>
      <div class="tools">
        <el-select v-model="selectedCourseId" placeholder="请选择课程" style="width: 260px" clearable>
          <el-option v-for="course in courses" :key="course.id" :label="course.courseName || `课程#${course.id}`" :value="course.id" />
        </el-select>
        <el-button type="primary" :loading="generating" @click="onGenerate">生成智能练习卷 (10题)</el-button>
      </div>
    </el-card>

    <el-empty v-if="!generating && questions.length === 0" description="请选择课程后生成练习题" />

    <div class="question-list" v-loading="generating">
      <el-card v-for="(question, index) in questions" :key="question.id" class="glass-card question-card" shadow="never">
        <div class="question-head">
          <div class="q-index">第 {{ index + 1 }} 题</div>
          <el-tag v-if="submitted" :type="isCorrect(question) ? 'success' : 'danger'">
            {{ isCorrect(question) ? '回答正确' : '回答错误' }}
          </el-tag>
        </div>
        <div class="q-content">{{ question.content || '-' }}</div>
        <el-radio-group v-model="answers[question.id]" :disabled="submitted" class="radio-group">
          <el-radio
            v-for="(op, idx) in optionItems(question.options)"
            :key="`${question.id}-${idx}`"
            :label="optionLabel(op)"
            class="option-row"
          >
            {{ op }}
          </el-radio>
        </el-radio-group>
        <div v-if="submitted" class="analysis-box">
          <div><span class="label">正确答案：</span><span class="right">{{ question.answer || '-' }}</span></div>
          <div><span class="label">解析：</span><span>{{ question.analysis || '-' }}</span></div>
        </div>
      </el-card>
    </div>

    <el-button
      v-if="questions.length > 0"
      type="success"
      size="large"
      class="submit-btn"
      @click="onSubmit"
    >
      提交并查看解析
    </el-button>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getCourses } from '@/api/course';
import { generatePractice } from '@/api/question';

const courses = ref([]);
const selectedCourseId = ref(null);
const questions = ref([]);
const generating = ref(false);
const submitted = ref(false);
const answers = ref({});

function optionItems(options) {
  if (!options || typeof options !== 'string') return [];
  return options.split(',').map(item => item.trim()).filter(Boolean);
}

function optionLabel(optionText) {
  return (optionText || '').split('.')[0]?.trim() || optionText;
}

function isCorrect(question) {
  return String(answers.value[question.id] || '').trim().toUpperCase() === String(question.answer || '').trim().toUpperCase();
}

async function loadCourses() {
  try {
    const data = await getCourses();
    courses.value = Array.isArray(data) ? data : [];
  } catch (e) {
    ElMessage.error(e.message || '加载课程失败');
    courses.value = [];
  }
}

async function onGenerate() {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择课程');
    return;
  }
  generating.value = true;
  try {
    const data = await generatePractice(selectedCourseId.value, 10);
    questions.value = Array.isArray(data) ? data : [];
    answers.value = {};
    submitted.value = false;
    ElMessage.success(`已生成 ${questions.value.length} 道练习题`);
  } catch (e) {
    ElMessage.error(e.message || '生成练习题失败');
    questions.value = [];
  } finally {
    generating.value = false;
  }
}

function onSubmit() {
  if (questions.value.length === 0) return;
  submitted.value = true;
}

onMounted(loadCourses);
</script>

<style scoped>
.student-smart-practice {
  display: grid;
  gap: 14px;
}
.top-panel {
  padding: 16px;
}
.title {
  font-size: 24px;
  color: #0F172A;
  font-weight: 700;
  margin-bottom: 12px;
}
.tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.question-list {
  display: grid;
  gap: 12px;
}
.question-card {
  padding: 12px;
}
.question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.q-index {
  color: #475569;
  font-weight: 600;
}
.q-content {
  color: #1E293B;
  line-height: 1.7;
  margin-bottom: 10px;
}
.radio-group {
  display: grid;
  gap: 8px;
}
.option-row {
  margin-right: 0;
}
.analysis-box {
  margin-top: 12px;
  background: rgba(248, 250, 252, 0.8);
  border-radius: 10px;
  padding: 10px;
  line-height: 1.7;
}
.label {
  color: #64748B;
}
.right {
  color: #188038;
  font-weight: 600;
}
.submit-btn {
  width: 100%;
}
</style>
