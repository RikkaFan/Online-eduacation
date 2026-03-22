<template>
  <div class="student-score-history">
    <div class="glass-card page-hero">
      <h2>我的成绩</h2>
      <div class="hero-sub">查看每次考试得分与交卷时间，支持快速回看试卷。</div>
    </div>
    <el-card class="glass-card table-card" shadow="never">
      <el-table :data="scores" v-loading="loading" empty-text="暂无成绩记录" stripe>
        <el-table-column label="课程名称" min-width="160">
          <template #default="{ row }">{{ row.exam?.course?.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="考试名称" min-width="180">
          <template #default="{ row }">{{ row.exam?.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="作答用时" width="140" align="center">
          <template #default="{ row }">{{ formatDuration(row.actualDurationSeconds) }}</template>
        </el-table-column>
        <el-table-column label="得分/满分" width="130" align="center">
          <template #default="{ row }">{{ scoreWithTotal(row) }}</template>
        </el-table-column>
        <el-table-column label="交卷时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain round @click="goReview(row)">查看试卷</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getMyScores } from '@/api/score';

const router = useRouter();
const scores = ref([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    scores.value = await getMyScores();
  } catch (e) {
    ElMessage.error(e.message || '加载我的成绩失败');
  } finally {
    loading.value = false;
  }
});

function goReview(row) {
  const examId = row?.examId || row?.exam?.id;
  if (!examId) return;
  router.push({ name: 'StudentExamReview', params: { id: examId } });
}

function formatDuration(seconds) {
  const sec = Number(seconds || 0);
  if (!Number.isFinite(sec) || sec <= 0) return '-';
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) return `${h}时${m}分${s}秒`;
  if (m > 0) return `${m}分${s}秒`;
  return `${s}秒`;
}

function scoreWithTotal(row) {
  const score = row?.score;
  const total = row?.exam?.totalScore;
  if (score == null && total == null) return '-/-';
  const left = score == null ? '-' : score;
  const right = total == null ? '-' : total;
  return `${left}/${right}`;
}

function formatDateTime(v) {
  if (!v) return '-';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (Number.isNaN(d.getTime())) return '-';
  return d.toLocaleString();
}
</script>

<style scoped>
.student-score-history {
  display: grid;
  gap: 20px;
  padding: 8px 4px 0;
}
.page-hero {
  padding: 22px 24px;
  border-radius: 20px !important;
  border: 1px solid rgba(212, 224, 244, 0.95);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03);
}
.page-hero h2 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
}
.hero-sub {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}
.table-card {
  border-radius: 20px !important;
  border: 1px solid rgba(212, 224, 244, 0.95);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03);
  padding: 18px;
}
</style>
