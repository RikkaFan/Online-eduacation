<template>
  <div class="exam-taking-page">
    <div class="top-nav glass-card">
      <div class="nav-title">{{ exam?.title || '考试作答' }}</div>
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button label="single">单题预览</el-radio-button>
        <el-radio-button label="all">整卷预览</el-radio-button>
      </el-radio-group>
    </div>
    <div class="exam-container" v-loading="loading">
      <aside class="left-panel glass-card">
        <div class="left-main">
          <div class="candidate">
            <div class="avatar">{{ userInitial }}</div>
            <div class="meta">
              <div class="name">{{ userName }}</div>
              <div class="role">考生</div>
            </div>
          </div>
          <div class="exam-meta">
            <div class="meta-row"><span>考试时间</span><strong>{{ timeRangeText }}</strong></div>
            <div class="meta-row"><span>考试时长</span><strong>{{ durationText }}</strong></div>
          </div>
          <div class="clock-card">
            <div class="clock-label">剩余时间</div>
            <div class="clock-value">{{ countdownText }}</div>
          </div>
          <el-tag v-if="switchCount > 0" type="danger" effect="dark">切屏警告 {{ switchCount }} / 3</el-tag>
        </div>
        <div class="left-footer">
          <el-button type="danger" class="submit-btn" :loading="submitting" @click="confirmSubmit">主动交卷</el-button>
        </div>
      </aside>
      <main class="center-panel glass-card">
        <div v-if="questionsWithIndex.length === 0" class="empty-text">该考试暂无题目</div>
        <template v-else-if="viewMode === 'single'">
          <div v-if="currentQuestion" class="single-wrap">
            <div class="q-stem-box">
              <div class="q-title">
                <span class="q-no">{{ currentQuestion.displayNumber }}.</span>
                <span class="q-content">{{ currentQuestion.content }}</span>
                <el-tag class="q-type" size="small">{{ typeName(currentQuestion.type) }}</el-tag>
              </div>
            </div>
            <template v-if="normalizeType(currentQuestion.type) === 'SUBJECTIVE'">
              <el-input
                v-model="currentQuestion.selectedAnswer"
                type="textarea"
                :rows="7"
                placeholder="请输入你的答案..."
                class="answer-input"
              />
            </template>
            <template v-else-if="normalizeType(currentQuestion.type) === 'JUDGE'">
              <el-radio-group v-model="currentQuestion.selectedAnswer" class="options-group">
                <el-radio label="T">正确 (True)</el-radio>
                <el-radio label="F">错误 (False)</el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="isMultipleType(currentQuestion.type)">
              <el-checkbox-group v-model="currentQuestion.selectedAnswerArray" class="options-group">
                <el-checkbox v-for="opt in parseOptions(currentQuestion)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else>
              <el-radio-group v-model="currentQuestion.selectedAnswer" class="options-group">
                <el-radio v-for="opt in parseOptions(currentQuestion)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-radio>
              </el-radio-group>
            </template>
            <div class="single-footer">
              <el-button class="nav-btn" size="large" :disabled="currentIndex <= 0" @click="prevQuestion">上一题</el-button>
              <el-button class="nav-btn" size="large" type="primary" :disabled="currentIndex >= questionsWithIndex.length - 1" @click="nextQuestion">下一题</el-button>
            </div>
          </div>
        </template>
        <div v-else class="all-wrap">
          <div v-for="q in questionsWithIndex" :key="q.id" class="question-item glass-card">
            <div class="q-title">
              <span class="q-no">{{ q.displayNumber }}.</span>
              <span class="q-content">{{ q.content }}</span>
              <el-tag class="q-type" size="small">{{ typeName(q.type) }}</el-tag>
            </div>
            <template v-if="normalizeType(q.type) === 'SUBJECTIVE'">
              <el-input v-model="q.selectedAnswer" type="textarea" :rows="5" placeholder="请输入你的答案..." class="answer-input" />
            </template>
            <template v-else-if="normalizeType(q.type) === 'JUDGE'">
              <el-radio-group v-model="q.selectedAnswer" class="options-group">
                <el-radio label="T">正确 (True)</el-radio>
                <el-radio label="F">错误 (False)</el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="isMultipleType(q.type)">
              <el-checkbox-group v-model="q.selectedAnswerArray" class="options-group">
                <el-checkbox v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else>
              <el-radio-group v-model="q.selectedAnswer" class="options-group">
                <el-radio v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">
                  {{ opt.key }}. {{ opt.text }}
                </el-radio>
              </el-radio-group>
            </template>
          </div>
        </div>
      </main>
      <aside class="right-panel glass-card">
        <div class="card-title">答题卡</div>
        <div class="legend">
          <span class="legend-dot current"></span><span class="legend-text">当前</span>
          <span class="legend-dot answered"></span><span class="legend-text">已答</span>
          <span class="legend-dot default"></span><span class="legend-text">未答</span>
        </div>
        <div v-if="singleQs.length > 0" class="group-block">
          <div class="group-title">单选题</div>
          <div class="answer-grid">
            <div
              v-for="q in singleQs"
              :key="`s-${q.id}`"
              class="grid-item"
              :class="{ current: currentIndex === q.originalIndex, answered: isAnswered(q) && currentIndex !== q.originalIndex }"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
        <div v-if="multipleQs.length > 0" class="group-block">
          <div class="group-title">多选题</div>
          <div class="answer-grid">
            <div
              v-for="q in multipleQs"
              :key="`m-${q.id}`"
              class="grid-item"
              :class="{ current: currentIndex === q.originalIndex, answered: isAnswered(q) && currentIndex !== q.originalIndex }"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
        <div v-if="judgeQs.length > 0" class="group-block">
          <div class="group-title">判断题</div>
          <div class="answer-grid">
            <div
              v-for="q in judgeQs"
              :key="`j-${q.id}`"
              class="grid-item"
              :class="{ current: currentIndex === q.originalIndex, answered: isAnswered(q) && currentIndex !== q.originalIndex }"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
        <div v-if="subjectiveQs.length > 0" class="group-block">
          <div class="group-title">主观题</div>
          <div class="answer-grid">
            <div
              v-for="q in subjectiveQs"
              :key="`sub-${q.id}`"
              class="grid-item"
              :class="{ current: currentIndex === q.originalIndex, answered: isAnswered(q) && currentIndex !== q.originalIndex }"
              @click="jumpToQuestion(q.originalIndex)"
            >{{ q.displayNumber }}</div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getExamDetail, submitExam as submitExamApi } from '@/api/examTaking';
import { parseLabeledOptions } from '@/utils/questionOptions';

const route = useRoute();
const router = useRouter();
const examId = Number(route.params.id);

const exam = ref(null);
const examQuestions = ref([]);
const loading = ref(false);
const submitting = ref(false);
const isSubmitted = ref(false);
const switchCount = ref(0);
const MAX_SWITCHES = 3;
const viewMode = ref('single');
const currentIndex = ref(0);

let timer = null;
const countdownMs = ref(0);
const countdownText = computed(() => {
  const ms = Math.max(0, countdownMs.value);
  const sec = Math.floor(ms / 1000) % 60;
  const min = Math.floor(ms / (1000 * 60)) % 60;
  const hr = Math.floor(ms / (1000 * 60 * 60));
  return `${String(hr).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
});
const timeRangeText = computed(() => {
  const s = formatDateTime(exam.value?.startTime);
  const e = formatDateTime(exam.value?.endTime);
  if (s === '-' && e === '-') return '-';
  return `${s} 至 ${e}`;
});
const durationText = computed(() => {
  const duration = Number(exam.value?.durationInMinutes || 0);
  if (Number.isFinite(duration) && duration > 0) return `${duration} 分钟`;
  return '-';
});

const questionsWithIndex = computed(() => examQuestions.value.map((q, i) => ({ ...q, originalIndex: i, displayNumber: i + 1 })));
const singleQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'SINGLE'));
const multipleQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'MULTIPLE'));
const judgeQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'JUDGE'));
const subjectiveQs = computed(() => questionsWithIndex.value.filter(q => normalizeType(q.type) === 'SUBJECTIVE'));
const currentQuestion = computed(() => questionsWithIndex.value[currentIndex.value]);

const userName = computed(() => {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user');
  if (!raw) return '考生';
  try {
    const user = JSON.parse(raw);
    return user?.username || user?.name || user?.user || '考生';
  } catch {
    return '考生';
  }
});
const userInitial = computed(() => String(userName.value || '考').trim().slice(0, 1).toUpperCase());

const isAnswered = (q) => {
  if (isMultipleType(q.type)) return Array.isArray(q.selectedAnswerArray) && q.selectedAnswerArray.length > 0;
  return !!String(q.selectedAnswer || '').trim();
};

onMounted(async () => {
  loading.value = true;
  try {
    const data = await getExamDetail(examId);
    exam.value = data;
    examQuestions.value = Array.isArray(data.questions) ? data.questions : [];
    setupInitialAnswers(examQuestions.value);
    startCountdown(data.endTime);
    currentIndex.value = 0;
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('beforeunload', handleUnload);
  } catch (e) {
    ElMessage.error(e.message || '加载考试详情失败');
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  window.removeEventListener('beforeunload', handleUnload);
});

onBeforeRouteLeave(async (_to, _from, next) => {
  if (isSubmitted.value) {
    next();
    return;
  }
  try {
    await ElMessageBox.confirm(
      '您正在离开考试界面！离开将自动提交您当前的试卷，且无法再次进入。是否确认强制交卷并离开？',
      '提示',
      {
        type: 'warning',
        confirmButtonText: '确认离开并交卷',
        cancelButtonText: '继续考试'
      }
    );
    const success = await submitExam(true);
    if (success) {
      next();
      return;
    }
    next(false);
  } catch {
    next(false);
  }
});

function handleVisibilityChange() {
  if (!document.hidden) return;
  switchCount.value += 1;
  if (switchCount.value < MAX_SWITCHES) {
    if (typeof ElMessageBox.warning === 'function') {
      ElMessageBox.warning(
        `【在线监考系统警告】您已离开考试页面！这是第 ${switchCount.value} 次警告。离开页面达到 3 次将强制交卷！`,
        '在线监考警告'
      );
    } else {
      ElMessageBox.alert(
        `【在线监考系统警告】您已离开考试页面！这是第 ${switchCount.value} 次警告。离开页面达到 3 次将强制交卷！`,
        '在线监考警告',
        { type: 'warning' }
      );
    }
    return;
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  ElMessageBox.alert(
    '您已超过最大切屏次数，系统已判定违规并强制交卷！',
    '违规交卷',
    {
      confirmButtonText: '确定',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false,
      type: 'error'
    }
  ).then(() => {
    submitExam();
  });
}

function setupInitialAnswers(list) {
  list.forEach(q => {
    if (isMultipleType(q.type)) {
      q.selectedAnswerArray = [];
      q.selectedAnswer = '';
    } else {
      q.selectedAnswer = '';
      q.selectedAnswerArray = [];
    }
  });
}

function handleUnload(e) {
  if (isSubmitted.value) return;
  e.preventDefault();
  e.returnValue = '';
}

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

function startCountdown(endTime) {
  let end = endTime ? new Date(endTime).getTime() : 0;
  if (!end && exam.value?.startTime && Number(exam.value?.durationInMinutes || 0) > 0) {
    const start = new Date(exam.value.startTime).getTime();
    if (start > 0) {
      end = start + Number(exam.value.durationInMinutes) * 60 * 1000;
    }
  }
  if (!end) {
    countdownMs.value = 0;
    return;
  }
  const tick = () => {
    countdownMs.value = end - Date.now();
    if (countdownMs.value <= 0) {
      clearInterval(timer);
      autoSubmit();
    }
  };
  tick();
  timer = setInterval(tick, 1000);
}

function formatDateTime(v) {
  if (!v) return '-';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString();
}

async function confirmSubmit() {
  try {
    await ElMessageBox.confirm('确认提交试卷吗？提交后不可更改。', '提示', { type: 'warning' });
    await doSubmit();
  } catch {
    return;
  }
}

async function autoSubmit() {
  if (submitting.value) return;
  await doSubmit(true, false);
}

async function doSubmit(isAuto = false, fromLeaveGuard = false) {
  submitting.value = true;
  try {
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    window.removeEventListener('beforeunload', handleUnload);
    const payload = examQuestions.value.map(q => {
      let ans = '';
      if (Array.isArray(q.selectedAnswerArray) && q.selectedAnswerArray.length > 0) {
        ans = [...q.selectedAnswerArray].sort().join(',');
      } else {
        ans = q.selectedAnswer || '';
      }
      return { question: { id: q.id }, selectedAnswer: ans };
    });
    const result = await submitExamApi(examId, payload);
    const score = result?.score ?? '未知';
    isSubmitted.value = true;
    if (timer) clearInterval(timer);
    countdownMs.value = 0;
    if (fromLeaveGuard) return true;
    await ElMessageBox.alert(`本次得分：${score}`, '提交成功', {
      type: 'success',
      confirmButtonText: '返回考试列表',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false
    });
    router.replace('/student/exams');
    return true;
  } catch (e) {
    ElMessage.error(e.message || '提交失败');
    return false;
  } finally {
    submitting.value = false;
  }
}

async function submitExam(fromLeaveGuard = false) {
  if (submitting.value) return;
  return doSubmit(true, fromLeaveGuard);
}
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
  gap: 14px;
}
.exam-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.35);
  padding-bottom: 6px;
}
.meta-row strong {
  color: #0f172a;
  margin-left: 10px;
  font-weight: 600;
}
.candidate {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: #fff;
  font-weight: 700;
  font-size: 20px;
}
.meta .name {
  font-weight: 600;
  color: #0f172a;
}
.meta .role {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}
.clock-card {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 12px;
}
.clock-label {
  color: #64748b;
  font-size: 13px;
}
.clock-value {
  margin-top: 6px;
  color: #0a84ff;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}
.left-footer {
  margin-top: auto;
}
.submit-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 10px;
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
  border: 1px solid #e2e8f0;
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
  margin-bottom: 16px;
  margin-right: 0;
}
.answer-input {
  margin-top: 4px;
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
.legend-dot.current {
  background: #0a84ff;
}
.legend-dot.answered {
  background: #dcfce7;
  border: 1px solid #22c55e;
}
.legend-dot.default {
  background: transparent;
  border: 1px solid #cbd5e1;
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
  background: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}
.grid-item.current {
  background: #0a84ff;
  border-color: #0a84ff;
  color: #fff;
}
.grid-item.answered {
  background: #dcfce7;
  border-color: #86efac;
  color: #15803d;
}
@media (max-width: 1280px) {
  .exam-container {
    grid-template-columns: 1fr;
    height: auto;
  }
  .left-panel {
    min-height: 220px;
  }
  .clock-value {
    font-size: 24px;
  }
}
</style>
