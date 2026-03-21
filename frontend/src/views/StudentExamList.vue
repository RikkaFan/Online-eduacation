<template>
  <div class="student-exam-list">
    <div class="glass-card page-hero">
      <div class="hero-left">
        <h2>考试列表</h2>
        <div class="hero-sub">查看考试安排、状态与进入入口</div>
      </div>
      <el-button class="hero-btn" round @click="goBackDashboard">返回首页</el-button>
    </div>
    <div class="glass-card list-card">
      <div class="list-head-row">
        <span>考试名称</span>
        <span>所属课程</span>
        <span>开始时间</span>
        <span>结束时间</span>
        <span>状态</span>
        <span>操作</span>
      </div>
      <div v-loading="loading" class="list-scroll">
        <template v-if="exams.length">
          <div v-for="row in exams" :key="row.id" class="exam-row glass-row">
            <div class="row-cell title-cell">
              <span class="exam-title">{{ row.title || '未命名考试' }}</span>
            </div>
            <div class="row-cell">{{ row.course?.courseName || row.courseName || '-' }}</div>
            <div class="row-cell">{{ formatDateTime(row.startTime) }}</div>
            <div class="row-cell">{{ formatDateTime(row.endTime) }}</div>
            <div class="row-cell">
              <el-tag effect="plain" :type="statusType(getStatus(row))">{{ getStatusText(getStatus(row)) }}</el-tag>
            </div>
            <div class="row-cell action-cell">
              <el-button type="primary" size="small" plain round :disabled="getStatus(row) !== 'ongoing'" @click="enterExam(row.id)">
                {{ getStatus(row) === 'finished' ? '已结束' : '进入考试' }}
              </el-button>
            </div>
          </div>
        </template>
        <el-empty v-else description="当前暂无考试" :image-size="84" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getAllExamsByAllCourses } from '@/api/examTaking';
import { getMyScores } from '@/api/score';

const router = useRouter();
const exams = ref([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    const [allExams, myScores] = await Promise.all([
      getAllExamsByAllCourses(),
      getMyScores().catch(() => []),
    ]);
    const submittedExamIds = new Set((myScores || []).map(s => s?.exam?.id).filter(Boolean));
    exams.value = (allExams || []).map(exam => ({
      ...exam,
      submitted: submittedExamIds.has(exam.id),
    }));
  } catch (e) {
    ElMessage.error(e.message || '加载考试失败');
    exams.value = [];
  } finally {
    loading.value = false;
  }
});

function formatDateTime(v) {
  if (!v) return '-';
  const d = typeof v === 'string' ? new Date(v) : v;
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleString();
}

function getStatus(exam) {
  if (exam.submitted) return 'finished';
  const now = Date.now();
  const start = exam.startTime ? new Date(exam.startTime).getTime() : 0;
  const end = exam.endTime ? new Date(exam.endTime).getTime() : 0;
  if (start && now < start) return 'pending';
  if (end && now > end) return 'finished';
  return 'ongoing';
}
function getStatusText(s) {
  return s === 'pending' ? '未开始' : s === 'finished' ? '已结束' : '进行中';
}
function statusType(s) {
  return s === 'pending' ? 'info' : s === 'finished' ? 'warning' : 'success';
}
function enterExam(id) {
  router.push(`/student/exam-ready/${id}`);
}

function goBackDashboard() {
  router.push('/student/dashboard');
}
</script>

<style scoped>
.student-exam-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 8px 4px 0;
}
.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 112px;
  padding: 0 28px;
  border-radius: 20px !important;
  position: relative;
  overflow: hidden;
}
.page-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(248, 252, 255, 0.96) 0%, rgba(240, 248, 255, 0.8) 64%, rgba(240, 248, 255, 0.28) 100%),
    repeating-linear-gradient(90deg, rgba(148, 163, 184, 0.1) 0 1px, transparent 1px 48px);
  pointer-events: none;
}
.hero-left {
  position: relative;
  z-index: 1;
}
.hero-left h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}
.hero-sub {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}
.hero-btn {
  position: relative;
  z-index: 1;
}
.list-card {
  border-radius: 20px !important;
  padding: 18px;
  min-height: 420px;
  display: flex;
  flex-direction: column;
}
.list-head-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.1fr) minmax(110px, 0.9fr) minmax(150px, 1fr) minmax(150px, 1fr) 90px 96px;
  gap: 12px;
  align-items: center;
  padding: 0 12px 10px;
  color: #8e8e93;
  font-size: 12px;
  font-weight: 600;
}
.list-scroll {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}
.exam-row {
  display: grid;
  grid-template-columns: minmax(160px, 1.1fr) minmax(110px, 0.9fr) minmax(150px, 1fr) minmax(150px, 1fr) 90px 96px;
  gap: 12px;
  align-items: center;
  padding: 14px 12px;
  border-radius: 16px;
  border: 1px solid rgba(212, 224, 244, 0.92);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05), 0 2px 6px rgba(15, 23, 42, 0.03);
}
.row-cell {
  color: #475569;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.title-cell .exam-title {
  color: #0f172a;
  font-weight: 600;
}
.action-cell {
  display: flex;
  justify-content: flex-start;
}
@media (max-width: 1200px) {
  .list-head-row,
  .exam-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .page-hero {
    min-height: 96px;
    padding: 0 20px;
  }
}
</style>
