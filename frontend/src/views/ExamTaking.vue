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
          <!-- 单选 -->
          <el-radio-group v-if="q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE'" v-model="studentAnswers[q.id]" class="option-group">
            <el-radio v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">{{ opt.label }}</el-radio>
          </el-radio-group>
          <!-- 多选 -->
          <el-checkbox-group v-else v-model="studentAnswersMulti[q.id]" class="option-group">
            <el-checkbox v-for="opt in parseOptions(q)" :key="opt.key" :label="opt.key">{{ opt.label }}</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getExamDetail, submitExam as submitExamApi } from '@/api/examTaking';

const route = useRoute();
const router = useRouter();
const examId = Number(route.params.id);

const exam = ref(null);
const examQuestions = ref([]);
const loading = ref(false);
const submitting = ref(false);
const switchCount = ref(0);
const MAX_SWITCHES = 3;

// 单选/判断：直接以 key 存储；多选：数组，提交时用逗号连接
const studentAnswers = reactive({});
const studentAnswersMulti = reactive({});

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
  } catch (e) {
    ElMessage.error(e.message || '加载考试详情失败');
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
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
    if (q.type === 'MULTIPLE_CHOICE') {
      studentAnswersMulti[q.id] = [];
    } else {
      studentAnswers[q.id] = '';
    }
  });
}

function typeName(t) {
  return t === 'SINGLE_CHOICE' ? '单选题' : t === 'MULTIPLE_CHOICE' ? '多选题' : '判断题';
}

function parseOptions(q) {
  if (q.type === 'TRUE_FALSE') {
    return [
      { key: 'T', label: '正确' },
      { key: 'F', label: '错误' },
    ];
  }
  const raw = q.options || '';
  // 解析如 "A:苹果, B:香蕉"
  return raw.split(',').map(s => s.trim()).filter(Boolean).map(chunk => {
    const idx = chunk.indexOf(':');
    if (idx > 0) {
      return { key: chunk.slice(0, idx).trim(), label: chunk.slice(idx + 1).trim() };
    }
    return { key: chunk, label: chunk };
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
  await doSubmit(true);
}

async function doSubmit(isAuto = false) {
  submitting.value = true;
  try {
    document.removeEventListener('visibilitychange', handleVisibilityChange);
    // 组装 payload: List<StudentAnswer> => [{question:{id}, selectedAnswer}]
    const payload = examQuestions.value.map(q => {
      let ans = '';
      if (q.type === 'MULTIPLE_CHOICE') {
        ans = (studentAnswersMulti[q.id] || []).join(',');
      } else {
        ans = studentAnswers[q.id] || '';
      }
      return { question: { id: q.id }, selectedAnswer: ans };
    });
    const result = await submitExamApi(examId, payload);
    const score = result?.score ?? '未知';
    ElMessageBox.alert(`本次得分：${score}`, '提交成功', { type: 'success' });
    setTimeout(() => router.push('/student/exams'), 1000);
  } catch (e) {
    ElMessage.error(e.message || '提交失败');
  } finally {
    submitting.value = false;
  }
}

async function submitExam() {
  if (submitting.value) return;
  await doSubmit(true);
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
.option-group { display: flex; flex-direction: column; gap: 8px; }
</style>
