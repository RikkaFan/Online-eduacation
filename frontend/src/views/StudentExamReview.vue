<template>
  <div class="exam-taking-page">
    <div class="top-nav glass-card">
      <div class="nav-title">{{ review.examTitle || '试卷回放' }}</div>
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button label="single">单题预览</el-radio-button>
        <el-radio-button label="all">整卷预览</el-radio-button>
      </el-radio-group>
    </div>
    <div class="exam-container" v-loading="loading">
      <aside class="left-panel glass-card">
        <div class="left-main">
          <div class="info-title">考试信息</div>
          <div class="info-item"><span>考试名称</span><strong>{{ review.examTitle || '-' }}</strong></div>
          <div class="info-item"><span>试卷总分</span><strong>{{ scoreText(review.totalScore) }}</strong></div>
          <div class="info-item"><span>学生得分</span><strong>{{ scoreText(review.studentScore) }}</strong></div>
          <div v-if="hasPendingSubjective" class="pending-tip">包含未批改的主观题，当前总分为暂定分</div>
        </div>
        <div class="left-footer">
          <el-button type="primary" class="submit-btn" @click="router.back()">返回成绩列表</el-button>
        </div>
      </aside>
      <main class="center-panel glass-card">
        <div v-if="questionsWithIndex.length === 0" class="empty-text">暂无试卷回放数据</div>
        <template v-else-if="viewMode === 'single'">
          <div v-if="currentQuestion" class="single-wrap">
            <div class="q-stem-box">
              <div class="q-title">
                <span class="q-no">{{ currentQuestion.displayNumber }}.</span>
                <span class="q-content">{{ currentQuestion.content }}</span>
                <el-tag class="q-type" size="small">{{ typeName(currentQuestion.type) }}</el-tag>
                <el-tag v-if="isPending(currentQuestion)" type="warning" effect="dark">
                  <span class="inline-icon-text"><el-icon><Clock /></el-icon>待批改</span>
                </el-tag>
                <el-tag v-else :type="scoreTagType(currentQuestion)" effect="dark">得分：{{ currentQuestion.score || 0 }} 分</el-tag>
              </div>
            </div>
            <template v-if="normalizeType(currentQuestion.type) === 'SUBJECTIVE'">
              <el-input
                v-model="currentQuestion.selectedAnswer"
                type="textarea"
                :rows="7"
                disabled
                placeholder="未作答"
                class="answer-input"
              />
            </template>
            <template v-else-if="normalizeType(currentQuestion.type) === 'JUDGE'">
              <el-radio-group v-model="currentQuestion.selectedAnswer" class="options-group" disabled>
                <el-radio label="T">正确 (True)</el-radio>
                <el-radio label="F">错误 (False)</el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="isMultipleType(currentQuestion.type)">
              <el-checkbox-group v-model="currentQuestion.selectedAnswerArray" class="options-group" disabled>
                <el-checkbox v-for="opt in parseOptions(currentQuestion)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else>
              <el-radio-group v-model="currentQuestion.selectedAnswer" class="options-group" disabled>
                <el-radio v-for="opt in parseOptions(currentQuestion)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-radio>
              </el-radio-group>
            </template>
            <div class="analysis-box">
              <div><span class="analysis-label"><el-icon><InfoFilled /></el-icon>标准答案：</span>{{ currentQuestion.answer || '-' }}</div>
              <div><span class="analysis-label"><el-icon><Tickets /></el-icon>解析：</span>{{ currentQuestion.analysis || '暂无解析' }}</div>
            </div>
            <div class="single-footer">
              <el-button class="nav-btn" size="large" :disabled="currentIndex <= 0" @click="prevQuestion">上一题</el-button>
              <el-button class="nav-btn" size="large" type="primary" :disabled="currentIndex >= questionsWithIndex.length - 1" @click="nextQuestion">下一题</el-button>
            </div>
          </div>
        </template>
        <div v-else class="all-wrap">
          <div v-for="q in questionsWithIndex" :key="q.questionId || q.id || q.displayNumber" class="question-item glass-card">
            <div class="q-title">
              <span class="q-no">{{ q.displayNumber }}.</span>
              <span class="q-content">{{ q.content }}</span>
              <el-tag class="q-type" size="small">{{ typeName(q.type) }}</el-tag>
              <el-tag v-if="isPending(q)" type="warning" effect="dark">
                <span class="inline-icon-text"><el-icon><Clock /></el-icon>待批改</span>
              </el-tag>
              <el-tag v-else :type="scoreTagType(q)" effect="dark">得分：{{ q.score || 0 }} 分</el-tag>
            </div>
            <template v-if="normalizeType(q.type) === 'SUBJECTIVE'">
              <el-input v-model="q.selectedAnswer" type="textarea" :rows="5" disabled placeholder="未作答" class="answer-input" />
            </template>
            <template v-else-if="normalizeType(q.type) === 'JUDGE'">
              <el-radio-group v-model="q.selectedAnswer" class="options-group" disabled>
                <el-radio label="T">正确 (True)</el-radio>
                <el-radio label="F">错误 (False)</el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="isMultipleType(q.type)">
              <el-checkbox-group v-model="q.selectedAnswerArray" class="options-group" disabled>
                <el-checkbox v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else>
              <el-radio-group v-model="q.selectedAnswer" class="options-group" disabled>
                <el-radio v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-radio>
              </el-radio-group>
            </template>
            <div class="analysis-box">
              <div><span class="analysis-label"><el-icon><InfoFilled /></el-icon>标准答案：</span>{{ q.answer || '-' }}</div>
              <div><span class="analysis-label"><el-icon><Tickets /></el-icon>解析：</span>{{ q.analysis || '暂无解析' }}</div>
            </div>
          </div>
        </div>
      </main>
      <aside class="right-panel glass-card">
        <div class="card-title">答题卡</div>
        <div class="legend">
          <span class="legend-dot right"></span><span class="legend-text">正确/得分</span>
          <span class="legend-dot wrong"></span><span class="legend-text">错误</span>
          <span class="legend-dot pending"></span><span class="legend-text">待批阅</span>
        </div>
        <div v-if="singleQs.length > 0" class="group-block">
          <div class="group-title">单选题</div>
          <div class="answer-grid">
            <div
              v-for="q in singleQs"
              :key="`s-${q.questionId || q.id}`"
              class="grid-item"
              :class="[scoreGridClass(q), { current: currentIndex === q.originalIndex }]"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
        <div v-if="multipleQs.length > 0" class="group-block">
          <div class="group-title">多选题</div>
          <div class="answer-grid">
            <div
              v-for="q in multipleQs"
              :key="`m-${q.questionId || q.id}`"
              class="grid-item"
              :class="[scoreGridClass(q), { current: currentIndex === q.originalIndex }]"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
        <div v-if="judgeQs.length > 0" class="group-block">
          <div class="group-title">判断题</div>
          <div class="answer-grid">
            <div
              v-for="q in judgeQs"
              :key="`j-${q.questionId || q.id}`"
              class="grid-item"
              :class="[scoreGridClass(q), { current: currentIndex === q.originalIndex }]"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
        <div v-if="subjectiveQs.length > 0" class="group-block">
          <div class="group-title">主观题</div>
          <div class="answer-grid">
            <div
              v-for="q in subjectiveQs"
              :key="`sub-${q.questionId || q.id}`"
              class="grid-item"
              :class="[scoreGridClass(q), { current: currentIndex === q.originalIndex }]"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Clock, InfoFilled, Tickets } from '@element-plus/icons-vue';
import { getExamReview } from '@/api/score';
import { parseLabeledOptions } from '@/utils/questionOptions';

const route = useRoute();
const router = useRouter();
const examId = Number(route.params.id);

const loading = ref(false);
const viewMode = ref('single');
const currentIndex = ref(0);
const review = ref({
  examTitle: '',
  totalScore: 0,
  studentScore: null,
  answers: []
});

const questionsWithIndex = computed(() => (review.value.answers || []).map((q, i) => ({ ...q, originalIndex: i, displayNumber: i + 1 })));
const singleQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'SINGLE'));
const multipleQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'MULTIPLE'));
const judgeQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'JUDGE'));
const subjectiveQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'SUBJECTIVE'));
const currentQuestion = computed(() => questionsWithIndex.value[currentIndex.value]);
const hasPendingSubjective = computed(() => subjectiveQs.value.some(q => q.score == null));

function normalizeType(type) {
  const normalized = String(type || '').toUpperCase();
  if (normalized === 'MULTIPLE' || normalized === 'MULTIPLE_CHOICE') return 'MULTIPLE';
  if (normalized === 'JUDGE' || normalized === 'TRUE_FALSE') return 'JUDGE';
  if (normalized === 'SUBJECTIVE') return 'SUBJECTIVE';
  return 'SINGLE';
}

function typeName(t) {
  const normalized = normalizeType(t);
  if (normalized === 'SUBJECTIVE') return '主观题';
  if (normalized === 'JUDGE') return '判断题';
  return normalized === 'MULTIPLE' ? '多选题' : '单选题';
}

function isMultipleType(type) {
  return normalizeType(type) === 'MULTIPLE';
}

function parseOptions(q) {
  return parseLabeledOptions(q.options || '');
}

function scoreText(score) {
  if (score == null) return '待批改';
  const value = Number(score);
  if (!Number.isFinite(value)) return '-';
  return `${value} 分`;
}

function scoreTagType(q) {
  return Number(q.score || 0) > 0 ? 'success' : 'danger';
}

function isPending(q) {
  return normalizeType(q.type) === 'SUBJECTIVE' && q.score == null;
}

function scoreGridClass(q) {
  if (q.score == null) return 'pending';
  return Number(q.score) > 0 ? 'right' : 'wrong';
}

function jumpToQuestion(index) {
  currentIndex.value = index;
  viewMode.value = 'single';
}

function prevQuestion() {
  if (currentIndex.value <= 0) return;
  currentIndex.value -= 1;
}

function nextQuestion() {
  if (currentIndex.value >= questionsWithIndex.value.length - 1) return;
  currentIndex.value += 1;
}

async function loadReview() {
  loading.value = true;
  try {
    const data = await getExamReview(examId);
    const answers = Array.isArray(data.answers) ? data.answers : [];
    review.value = {
      examTitle: data.examTitle || '',
      totalScore: data.totalScore ?? 0,
      studentScore: data.studentScore ?? null,
      answers: answers.map(item => ({
        ...item,
        selectedAnswer: item.selectedAnswer || '',
        selectedAnswerArray: isMultipleType(item.type)
          ? String(item.selectedAnswer || '').split(',').map(s => s.trim()).filter(Boolean)
          : []
      }))
    };
    currentIndex.value = 0;
  } catch (e) {
    ElMessage.error(e.message || '加载试卷回放失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadReview);
</script>

<style scoped>
.exam-taking-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 10px;
}
.top-nav {
  min-height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
}
.nav-title {
  font-size: 18px;
  color: #0f172a;
  font-weight: 700;
}
.exam-container {
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  gap: 20px;
  height: calc(100vh - 100px);
  padding: 0 20px 20px;
}
.left-panel {
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.left-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.info-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #475569;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.35);
  padding-bottom: 8px;
}
.info-item strong {
  color: #0f172a;
}
.pending-tip {
  color: #dc2626;
  font-size: 12px;
  line-height: 1.5;
}
.left-footer {
  margin-top: auto;
}
.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-weight: 600;
}
.center-panel {
  min-width: 0;
  border-radius: 16px;
  padding: 20px;
  overflow-y: auto;
}
.empty-text {
  color: #64748b;
}
.single-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.all-wrap {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.question-item {
  border-radius: 12px;
  padding: 18px;
}
.q-stem-box {
  background: rgba(241, 245, 249, 0.9);
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  padding: 14px 16px;
}
.q-title {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.q-no {
  color: #0a84ff;
  font-weight: 700;
}
.q-content {
  flex: 1;
  color: #0f172a;
  line-height: 1.7;
}
.q-type {
  margin-left: auto;
}
.options-group {
  display: flex;
  flex-direction: column;
}
.options-group :deep(.el-radio),
.options-group :deep(.el-checkbox) {
  margin-bottom: 12px;
  margin-right: 0;
}
.answer-input {
  margin-top: 4px;
}
.analysis-box {
  margin-top: 8px;
  background: #f2f2f7;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.7;
  color: #334155;
}
.analysis-label {
  color: #0f172a;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.inline-icon-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.single-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.nav-btn {
  flex: 1;
  height: 46px;
  font-weight: 600;
}
.right-panel {
  border-radius: 16px;
  padding: 16px;
  overflow: auto;
}
.card-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}
.legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  margin-bottom: 14px;
}
.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}
.legend-dot.right {
  background: #22c55e;
}
.legend-dot.wrong {
  background: #ef4444;
}
.legend-dot.pending {
  background: #f59e0b;
}
.legend-text {
  color: #64748b;
  font-size: 12px;
  margin-right: 6px;
}
.group-block {
  margin-bottom: 20px;
}
.group-title {
  color: #334155;
  font-size: 14px;
  margin-bottom: 10px;
  font-weight: 700;
}
.answer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(36px, 1fr));
  gap: 10px;
  margin-bottom: 20px;
}
.grid-item {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  border: 1px solid #d1d1d6;
  cursor: pointer;
  font-weight: 500;
  color: #1c1c1e;
  font-size: 14px;
}
.grid-item.current {
  box-shadow: 0 0 0 2px rgba(10, 132, 255, 0.35) inset;
}
.grid-item.right {
  background: #dcfce7;
  border-color: #86efac;
  color: #166534;
}
.grid-item.wrong {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #b91c1c;
}
.grid-item.pending {
  background: #fef3c7;
  border-color: #fcd34d;
  color: #92400e;
}
@media (max-width: 1280px) {
  .exam-container {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>
