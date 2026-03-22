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
      <main class="center-panel glass-card" :class="{ 'all-mode': viewMode === 'all' }">
        <div v-if="questionsWithIndex.length === 0" class="empty-text">该考试暂无题目</div>
        <template v-else-if="viewMode === 'single'">
          <div v-if="currentQuestion" class="single-wrap">
            <div class="single-content">
              <div class="q-stem-box">
                <div class="q-meta-line">{{ questionMeta(currentQuestion) }}</div>
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
                  <el-radio class="option-row" label="T">
                    <span class="option-key">T</span>
                    <span class="option-text">正确 (True)</span>
                  </el-radio>
                  <el-radio class="option-row" label="F">
                    <span class="option-key">F</span>
                    <span class="option-text">错误 (False)</span>
                  </el-radio>
                </el-radio-group>
              </template>
              <template v-else-if="isMultipleType(currentQuestion.type)">
                <el-checkbox-group v-model="currentQuestion.selectedAnswerArray" class="options-group">
                  <el-checkbox class="option-row" v-for="opt in parseOptions(currentQuestion)" :key="opt.key" :label="opt.key">
                    <span class="option-key">{{ opt.key }}</span>
                    <span class="option-text">{{ opt.text }}</span>
                  </el-checkbox>
                </el-checkbox-group>
              </template>
              <template v-else>
                <el-radio-group v-model="currentQuestion.selectedAnswer" class="options-group">
                  <el-radio class="option-row" v-for="opt in parseOptions(currentQuestion)" :key="opt.key" :label="opt.key">
                    <span class="option-key">{{ opt.key }}</span>
                    <span class="option-text">{{ opt.text }}</span>
                  </el-radio>
                </el-radio-group>
              </template>
            </div>
            <div class="single-footer">
              <el-button class="nav-btn" size="large" :disabled="currentIndex <= 0" @click="prevQuestion">上一题</el-button>
              <el-button class="nav-btn" size="large" type="primary" :disabled="currentIndex >= questionsWithIndex.length - 1" @click="nextQuestion">下一题</el-button>
            </div>
          </div>
        </template>
        <div v-else class="all-wrap">
          <div v-for="q in questionsWithIndex" :key="q.id" class="question-item glass-card">
            <div class="q-meta-line">{{ questionMeta(q) }}</div>
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
                <el-radio class="option-row" label="T">
                  <span class="option-key">T</span>
                  <span class="option-text">正确 (True)</span>
                </el-radio>
                <el-radio class="option-row" label="F">
                  <span class="option-key">F</span>
                  <span class="option-text">错误 (False)</span>
                </el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="isMultipleType(q.type)">
              <el-checkbox-group v-model="q.selectedAnswerArray" class="options-group">
                <el-checkbox class="option-row" v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">
                  <span class="option-key">{{ opt.key }}</span>
                  <span class="option-text">{{ opt.text }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else>
              <el-radio-group v-model="q.selectedAnswer" class="options-group">
                <el-radio class="option-row" v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">
                  <span class="option-key">{{ opt.key }}</span>
                  <span class="option-text">{{ opt.text }}</span>
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
          <div class="group-title">单选题 <span class="group-count">{{ singleQs.length }}</span></div>
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
          <div class="group-title">多选题 <span class="group-count">{{ multipleQs.length }}</span></div>
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
          <div class="group-title">判断题 <span class="group-count">{{ judgeQs.length }}</span></div>
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
          <div class="group-title">主观题 <span class="group-count">{{ subjectiveQs.length }}</span></div>
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

const questionsWithIndex = computed(() => examQuestions.value);
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
  list.forEach((q, i) => {
    if (isMultipleType(q.type)) {
      q.selectedAnswerArray = [];
      q.selectedAnswer = '';
    } else {
      q.selectedAnswer = '';
      q.selectedAnswerArray = [];
    }
    q.originalIndex = i;
    q.displayNumber = i + 1;
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

function questionMeta(q) {
  const score = Number(q?.score || 0);
  const scoreText = Number.isFinite(score) && score > 0 ? `${score}分` : '未标分';
  return `第 ${q?.displayNumber || '-'} 题 · ${typeName(q?.type)} · ${scoreText}`;
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
  const durationMinutes = Number(exam.value?.durationInMinutes || 0);
  if (Number.isFinite(durationMinutes) && durationMinutes > 0) {
    countdownMs.value = durationMinutes * 60 * 1000;
  } else {
    const start = exam.value?.startTime ? new Date(exam.value.startTime).getTime() : 0;
    const end = endTime ? new Date(endTime).getTime() : 0;
    if (start > 0 && end > start) {
      countdownMs.value = end - start;
    }
  }
  if (countdownMs.value <= 0) {
    countdownMs.value = 0;
    return;
  }
  const tick = () => {
    countdownMs.value -= 1000;
    if (countdownMs.value <= 0) {
      countdownMs.value = 0;
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
  --glass-bg: rgba(255, 255, 255, 0.8);
  --glass-border: rgba(212, 224, 244, 0.92);
  --glass-shadow: 0 14px 30px rgba(15, 23, 42, 0.06), 0 3px 10px rgba(148, 163, 184, 0.18);
  min-height: calc(100vh - 12px);
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px 20px 20px;
  box-sizing: border-box;
  background:
    radial-gradient(1200px 520px at 6% 0%, rgba(96, 165, 250, 0.16), transparent 60%),
    radial-gradient(1000px 560px at 92% -8%, rgba(56, 189, 248, 0.14), transparent 65%),
    linear-gradient(180deg, rgba(248, 252, 255, 0.7), rgba(248, 252, 255, 0.58));
}
.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(20px) saturate(180%);
  border-radius: 24px;
}
.top-nav {
  min-height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
}
.nav-title {
  font-size: 20px;
  color: #1d1d1f;
  font-weight: 700;
}
.exam-container {
  display: grid;
  grid-template-columns: minmax(236px, 260px) 1fr minmax(268px, 292px);
  gap: 18px;
  min-height: 0;
  flex: 1;
}
.left-panel {
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.left-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  font-size: 13px;
  color: #64748b;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.35);
  padding-bottom: 10px;
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
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(212, 224, 244, 0.9);
  border-radius: 16px;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
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
  letter-spacing: 0.02em;
  text-align: center;
}
.left-footer {
  margin-top: auto;
}
.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 14px;
}
.center-panel {
  min-width: 0;
  padding: 24px 24px 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.empty-text {
  color: #64748b;
}
.single-wrap {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  gap: 16px;
}
.single-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.all-wrap {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.question-item {
  border: 1px solid rgba(212, 224, 244, 0.88);
  border-radius: 20px;
  padding: 18px 18px 16px;
}
.q-stem-box {
  background: rgba(248, 252, 255, 0.88);
  border: 1px solid rgba(212, 224, 244, 0.92);
  border-radius: 18px;
  padding: 14px 16px;
}
.q-title {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  column-gap: 12px;
}
.q-meta-line {
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 9px;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.q-no {
  color: #0a84ff;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.38;
  margin-top: -1px;
}
.q-content {
  color: #0f172a;
  line-height: 1.62;
  font-size: 17px;
  font-weight: 600;
}
.q-type {
  margin-top: 2px;
}
.options-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 16px;
  padding: 4px 2px;
}
.options-group :deep(.el-radio.option-row),
.options-group :deep(.el-checkbox.option-row) {
  width: 100%;
  margin: 0;
  margin-right: 0;
  border: 1px solid rgba(220, 230, 246, 0.95);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  transition: all 0.2s ease;
  padding: 6px 10px;
  box-sizing: border-box;
  min-height: 52px;
  box-shadow: 0 6px 14px rgba(148, 163, 184, 0.08);
}
.options-group :deep(.el-radio.option-row:hover),
.options-group :deep(.el-checkbox.option-row:hover) {
  transform: translateY(-1px);
  border-color: rgba(10, 132, 255, 0.24);
}
.options-group :deep(.el-radio__input),
.options-group :deep(.el-checkbox__input) {
  display: none;
}
.options-group :deep(.el-radio__label),
.options-group :deep(.el-checkbox__label) {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #1e293b;
  font-size: 16px;
  line-height: 1.56;
  padding: 0;
  white-space: normal;
}
.options-group :deep(.is-checked.option-row) {
  border-color: rgba(10, 132, 255, 0.38);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(239, 246, 255, 0.84));
  box-shadow: 0 10px 18px rgba(10, 132, 255, 0.14);
}
.option-key {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 999px;
  border: 1.5px solid rgba(191, 219, 254, 0.95);
  background: rgba(255, 255, 255, 0.92);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  color: #64748b;
}
.options-group :deep(.is-checked.option-row .option-key) {
  border-color: rgba(10, 132, 255, 0.72);
  background: rgba(255, 255, 255, 0.98);
  color: #0a84ff;
  box-shadow: 0 6px 12px rgba(10, 132, 255, 0.18);
}
.option-text {
  flex: 1;
  margin-top: 0;
  text-align: left;
}
.answer-input {
  margin-top: 4px;
}
.single-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: auto;
  padding-top: 14px;
}
.nav-btn {
  height: 46px;
  font-weight: 600;
  border-radius: 16px;
}
.all-mode .all-wrap {
  scrollbar-gutter: stable;
}
.right-panel {
  padding: 18px 16px;
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
.group-count {
  color: #0a84ff;
  margin-left: 6px;
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
  border-radius: 10px;
  border: 1px solid rgba(209, 213, 219, 0.95);
  cursor: pointer;
  font-weight: 500;
  color: #1c1c1e;
  background: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  transition: all 0.2s ease;
}
.grid-item.current {
  background: #0a84ff;
  border-color: #0a84ff;
  color: #fff;
  box-shadow: 0 6px 14px rgba(10, 132, 255, 0.32);
}
.grid-item.answered {
  background: #dcfce7;
  border-color: #86efac;
  color: #15803d;
}
@media (max-width: 1280px) {
  .exam-taking-page {
    padding: 14px 14px 18px;
  }
  .exam-container {
    grid-template-columns: 1fr;
    min-height: auto;
  }
  .left-panel {
    min-height: 220px;
  }
  .center-panel {
    min-height: 520px;
  }
  .clock-value {
    font-size: 24px;
  }
}
@media (max-width: 760px) {
  .center-panel {
    padding: 18px 14px;
  }
  .single-footer {
    grid-template-columns: 1fr;
  }
  .q-content {
    font-size: 17px;
  }
  .options-group :deep(.el-radio__label),
  .options-group :deep(.el-checkbox__label) {
    font-size: 15px;
  }
  .option-key {
    width: 34px;
    height: 34px;
    font-size: 18px;
  }
}
</style>
