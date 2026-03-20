<template>
  <div class="exam-taking">
    <div class="exam-header">
      <div class="title">{{ exam?.title || '-' }}</div>
      <div class="header-center">
        <span class="countdown">剩余时间：{{ countdownText }}</span>
        <el-tag v-if="switchCount > 0" type="danger" effect="dark" style="margin-left: 16px;">切屏警告: {{ switchCount }} / 3 次</el-tag>
      </div>
      <el-button type="danger" size="small" @click="confirmSubmit" :loading="submitting">主动交卷</el-button>
    </div>

    <el-card shadow="never" class="mt-12" v-loading="loading">
      <div v-if="examQuestions.length === 0">该考试暂无题目</div>
      <div v-else class="question-list">
        <div v-for="(q, idx) in examQuestions" :key="q.id" class="question-item">
          <div class="q-title">
            <span class="q-no">{{ idx + 1 }}.</span>
            <span>{{ q.content }}</span>
            <el-tag class="q-type" size="small">{{ typeName(q.type) }}</el-tag>
          </div>
          <el-radio-group v-if="!isMultipleType(q.type)" v-model="q.selectedAnswer" class="options-group">
            <el-radio v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">{{ opt.key }}. {{ opt.text }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else v-model="q.selectedAnswerArray" class="options-group">
            <el-checkbox v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">{{ opt.key }}. {{ opt.text }}</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getExamDetail, submitExam as submitExamApi } from '@/api/examTaking';

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

// 倒计时
let timer = null;
const countdownMs = ref(0);
const countdownText = computed(() => {
  const ms = Math.max(0, countdownMs.value);
  const sec = Math.floor(ms / 1000) % 60;
  const min = Math.floor(ms / (1000 * 60)) % 60;
  const hr = Math.floor(ms / (1000 * 60 * 60));
  return `${String(hr).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
});

onMounted(async () => {
  loading.value = true;
  try {
    const data = await getExamDetail(examId);
    exam.value = data;
    examQuestions.value = Array.isArray(data.questions) ? data.questions : [];
    setupInitialAnswers(examQuestions.value);
    startCountdown(data.endTime);
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
      type: 'error',
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

function typeName(t) {
  return isMultipleType(t) ? '多选题' : '单选题';
}

function isMultipleType(type) {
  const normalized = String(type || '').toUpperCase();
  return normalized === 'MULTIPLE' || normalized === 'MULTIPLE_CHOICE';
}

function parseOptions(q) {
  const raw = q.options || '';
  return raw.split(',').map(s => s.trim()).filter(Boolean).map(chunk => {
    const idx = chunk.indexOf(':');
    if (idx > 0) {
      return { key: chunk.slice(0, idx).trim(), text: chunk.slice(idx + 1).trim() };
    }
    const dotIndex = chunk.indexOf('.');
    if (dotIndex > 0) {
      return { key: chunk.slice(0, dotIndex).trim(), text: chunk.slice(dotIndex + 1).trim() };
    }
    return { key: chunk, text: chunk };
  });
}

function startCountdown(endTime) {
  const end = endTime ? new Date(endTime).getTime() : 0;
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

async function confirmSubmit() {
  try {
    await ElMessageBox.confirm('确认提交试卷吗？提交后不可更改。', '提示', { type: 'warning' });
    await doSubmit();
  } catch {
    // user cancelled
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
    // 组装 payload: List<StudentAnswer> => [{question:{id}, selectedAnswer}]
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
    if (fromLeaveGuard) {
      return true;
    }
    await ElMessageBox.alert(`本次得分：${score}`, '提交成功', {
      type: 'success',
      confirmButtonText: '返回考试列表',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false,
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
.exam-taking {
  padding: 0 20px 20px 20px;
}
.exam-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffffcc;
  padding: 12px 16px;
  border-radius: 14px;
  margin: 12px 0;
  backdrop-filter: saturate(180%) blur(10px);
  box-shadow: 0 10px 30px rgba(0,0,0,.06);
}
.header-center {
  display: flex;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.mt-12 { margin-top: 12px; }
.question-list { display: flex; flex-direction: column; gap: 16px; }
.question-item { padding: 12px; border-radius: 12px; background: #fff; border: 1px solid #f2f2f7; }
.q-title { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.q-no { color: #0a84ff; font-weight: 600; }
.q-type { margin-left: auto; }
.options-group { display: flex; flex-direction: column; gap: 8px; }
</style>
