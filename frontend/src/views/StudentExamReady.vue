<template>
  <div class="exam-ready-page">
    <div class="exam-ready-card glass-card" v-loading="loading">
      <div class="ready-title">考前准备</div>
      <div class="ready-exam-name">{{ exam?.title || '考试信息加载中...' }}</div>
      <el-descriptions border :column="1" class="ready-desc">
        <el-descriptions-item>
          <template #label>
            <span class="label-wrap">
              <el-icon><Reading /></el-icon>
              <span>考试科目</span>
            </span>
          </template>
          {{ courseName }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="label-wrap">
              <el-icon><Clock /></el-icon>
              <span>考试时长</span>
            </span>
          </template>
          {{ durationText }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="label-wrap">
              <el-icon><Clock /></el-icon>
              <span>考试时间</span>
            </span>
          </template>
          {{ timeRangeText }}
        </el-descriptions-item>
        <el-descriptions-item>
          <template #label>
            <span class="label-wrap">
              <el-icon><Histogram /></el-icon>
              <span>总分分布</span>
            </span>
          </template>
          {{ scoreDistributionText }}
        </el-descriptions-item>
      </el-descriptions>
      <el-button type="primary" size="large" class="enter-btn" :loading="entering" @click="onReady">
        准备就绪，进入考场
      </el-button>
      <div class="ready-hint">* 点击进入后将开始计时并开启防作弊监测</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Clock, Reading, Histogram } from '@element-plus/icons-vue';
import { getExamDetail } from '@/api/examTaking';

const route = useRoute();
const router = useRouter();
const examId = Number(route.params.id);

const loading = ref(false);
const entering = ref(false);
const exam = ref(null);

const passScore = computed(() => {
  const total = Number(exam.value?.totalScore || 0);
  if (!Number.isFinite(total) || total <= 0) return 0;
  return Math.ceil(total * 0.6);
});

const durationText = computed(() => {
  const m = Number(exam.value?.durationInMinutes || 0);
  if (Number.isFinite(m) && m > 0) return `${m} 分钟`;
  const start = exam.value?.startTime ? new Date(exam.value.startTime).getTime() : 0;
  const end = exam.value?.endTime ? new Date(exam.value.endTime).getTime() : 0;
  if (start > 0 && end > start) {
    const minutes = Math.round((end - start) / 60000);
    return `${minutes} 分钟`;
  }
  return '-';
});

const timeRangeText = computed(() => {
  const s = formatDateTime(exam.value?.startTime);
  const e = formatDateTime(exam.value?.endTime);
  if (s === '-' && e === '-') return '-';
  return `${s} 至 ${e}`;
});

const courseName = computed(() => {
  return exam.value?.course?.courseName || exam.value?.courseName || '-';
});

const scoreDistributionText = computed(() => {
  return `总分 ${scoreText(exam.value?.totalScore)} ｜ 及格 ${scoreText(passScore.value)}`;
});

function scoreText(val) {
  const num = Number(val);
  if (!Number.isFinite(num) || num <= 0) return '-';
  return `${num} 分`;
}

function formatDateTime(v) {
  if (!v) return '-';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString();
}

async function loadExam() {
  loading.value = true;
  try {
    exam.value = await getExamDetail(examId);
  } catch (e) {
    ElMessage.error(e.message || '加载考试信息失败');
  } finally {
    loading.value = false;
  }
}

async function onReady() {
  entering.value = true;
  try {
    await ElMessageBox.confirm(
      '1. 本场考试已开启在线监考，切屏行为将被记录并计次。<br/><br/>2. 考试开始后倒计时持续进行，到时系统将自动交卷。<br/><br/>3. 离开考试页面将触发强制交卷，请全程保持专注作答。<br/><br/>是否确认进入考场？',
      '考试开始前确认',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确认进入',
        cancelButtonText: '暂不进入',
        type: 'warning'
      }
    );
    router.push('/student/exam-taking/' + examId);
  } catch {
    return;
  } finally {
    entering.value = false;
  }
}

onMounted(loadExam);
</script>

<style scoped>
.exam-ready-page {
  --glass-bg: rgba(255, 255, 255, 0.84);
  --glass-border: rgba(212, 224, 244, 0.95);
  --glass-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03);
  min-height: calc(100vh - 12px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 36px 24px 24px;
}
.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(16px) saturate(180%);
  border-radius: 22px;
}
.exam-ready-card {
  width: min(680px, 100%);
  padding: 34px 30px 30px;
  text-align: center;
}
.ready-title {
  font-size: 30px;
  font-weight: 700;
  color: #1c1c1e;
  margin-bottom: 24px;
}
.ready-exam-name {
  margin-bottom: 20px;
  font-size: 24px;
  color: #1e293b;
  font-weight: 600;
  line-height: 1.5;
}
.ready-desc {
  margin: 0 auto;
  width: 88%;
  margin-bottom: 34px;
}
.label-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-weight: 600;
}
.label-wrap .el-icon {
  color: #0a84ff;
}
.enter-btn {
  width: 88%;
  margin: 0 auto;
  display: block;
  height: 50px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 16px;
  background: linear-gradient(135deg, #0a84ff, #3ea0ff);
  border: none;
  box-shadow: 0 10px 20px rgba(10, 132, 255, 0.24);
  animation: pulseGlow 2.2s ease-in-out infinite;
}
.ready-hint {
  color: #8e8e93;
  font-size: 13px;
  margin-top: 16px;
}
@media (max-width: 760px) {
  .exam-ready-card {
    width: min(94vw, 680px);
    padding: 24px 16px 22px;
  }
  .ready-desc,
  .enter-btn {
    width: 92%;
  }
}
@keyframes pulseGlow {
  0% { transform: translateY(0); box-shadow: 0 10px 20px rgba(10, 132, 255, 0.22); }
  50% { transform: translateY(-1px); box-shadow: 0 14px 26px rgba(10, 132, 255, 0.34); }
  100% { transform: translateY(0); box-shadow: 0 10px 20px rgba(10, 132, 255, 0.22); }
}
</style>
