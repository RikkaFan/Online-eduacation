<template>
  <div class="student-smart-practice">
    <el-card class="glass-card top-panel" shadow="never">
      <div class="title">自主刷题</div>
      <div class="sub-title">按课程生成练习卷，提交后即时查看答案解析。</div>
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
        <template v-if="isSubjective(question)">
          <el-input
            v-model="answers[question.id]"
            type="textarea"
            :rows="4"
            placeholder="请输入你的作答内容"
            :disabled="submitted"
          />
        </template>
        <template v-else-if="isJudge(question)">
          <el-radio-group v-model="answers[question.id]" :disabled="submitted" class="radio-group">
            <el-radio label="T" class="option-row">T. 正确</el-radio>
            <el-radio label="F" class="option-row">F. 错误</el-radio>
          </el-radio-group>
        </template>
        <template v-else-if="isMultiple(question)">
          <el-checkbox-group v-model="answers[question.id]" :disabled="submitted" class="radio-group">
            <el-checkbox
              v-for="(op, idx) in optionItems(question.options)"
              :key="`${question.id}-${idx}`"
              :label="optionLabel(op)"
              class="option-row"
            >
              {{ op }}
            </el-checkbox>
          </el-checkbox-group>
        </template>
        <template v-else>
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
        </template>
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
import { getMyEnrolledCourses } from '@/api/course';
import { generatePractice } from '@/api/question';
import { parseOptionDisplayItems } from '@/utils/questionOptions';

const courses = ref([]);
const selectedCourseId = ref(null);
const questions = ref([]);
const generating = ref(false);
const submitted = ref(false);
const answers = ref({});

function optionItems(options) {
  return parseOptionDisplayItems(options || '');
}

function optionLabel(optionText) {
  return (optionText || '').split('.')[0]?.trim() || optionText;
}

function normalizeType(type) {
  const t = String(type || '').trim().toUpperCase();
  if (t === 'SINGLE' || t === 'SINGLE_CHOICE') return 'SINGLE';
  if (t === 'MULTIPLE' || t === 'MULTIPLE_CHOICE') return 'MULTIPLE';
  if (t === 'TRUE_FALSE' || t === 'JUDGE') return 'JUDGE';
  if (t === 'SUBJECTIVE') return 'SUBJECTIVE';
  return t;
}

function isMultiple(question) {
  return normalizeType(question?.type) === 'MULTIPLE';
}

function isJudge(question) {
  return normalizeType(question?.type) === 'JUDGE';
}

function isSubjective(question) {
  return normalizeType(question?.type) === 'SUBJECTIVE';
}

function normalizeAnswerValue(question, value) {
  if (isMultiple(question)) {
    const arr = Array.isArray(value) ? value : String(value || '').split(',').map(v => v.trim()).filter(Boolean);
    return arr.map(v => String(v).trim().toUpperCase()).sort().join(',');
  }
  return String(value || '').trim().toUpperCase();
}

function isCorrect(question) {
  return normalizeAnswerValue(question, answers.value[question.id]) === normalizeAnswerValue(question, question.answer);
}

async function loadCourses() {
  try {
    const data = await getMyEnrolledCourses();
    courses.value = Array.isArray(data) ? data : [];
    if (!courses.value.length) {
      ElMessage.warning('暂无已选课程，请先在“我的课程”选课');
    }
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
    const answerInit = {};
    questions.value.forEach((question) => {
      answerInit[question.id] = isMultiple(question) ? [] : '';
    });
    answers.value = answerInit;
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
  const hasEmpty = questions.value.some((question) => {
    const val = answers.value[question.id];
    if (isMultiple(question)) return !Array.isArray(val) || val.length === 0;
    return String(val || '').trim() === '';
  });
  if (hasEmpty) {
    ElMessage.warning('请先完成全部题目的作答');
    return;
  }
  submitted.value = true;
}

onMounted(loadCourses);
</script>

<style scoped>
.student-smart-practice {
  display: grid;
  gap: 20px;
  padding: 8px 4px 0;
}
.glass-card {
  border-radius: 20px !important;
  border: 1px solid rgba(212, 224, 244, 0.95) !important;
  background: rgba(255, 255, 255, 0.84) !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03) !important;
}
.top-panel {
  padding: 22px 24px;
}
.title {
  font-size: 26px;
  color: #0F172A;
  font-weight: 700;
}
.sub-title {
  margin-top: 8px;
  margin-bottom: 16px;
  color: #64748b;
  font-size: 13px;
}
.tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.question-list {
  display: grid;
  gap: 16px;
}
.question-card {
  padding: 18px;
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
  background: rgba(248, 250, 252, 0.9);
  border-radius: 12px;
  padding: 12px;
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
