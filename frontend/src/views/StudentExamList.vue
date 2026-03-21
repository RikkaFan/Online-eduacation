<template>
  <div class="student-exam-list">
    <div class="page-header glass-card">
      <div class="header-left">
        <h2>考试列表</h2>
        <div class="header-sub">查看考试安排、状态与进入入口</div>
      </div>
      <el-button round @click="goBackDashboard">返回首页</el-button>
    </div>
    <el-card class="glass-card list-card" shadow="never">
      <el-table :data="exams" v-loading="loading" style="width: 100%" empty-text="当前暂无考试">
        <el-table-column prop="title" label="考试名称" min-width="200" />
        <el-table-column label="所属课程" min-width="160">
          <template #default="{ row }">
            {{ row.course?.courseName || row.courseName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(getStatus(row))">{{ getStatusText(getStatus(row)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" :disabled="getStatus(row) !== 'ongoing'" @click="enterExam(row.id)">
              {{ getStatus(row) === 'finished' ? '已结束' : '进入考试' }}
            </el-button>
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
  ElMessage.info('考试入口保持当前页展示，不执行跳转');
}

function goBackDashboard() {
  router.push('/student/dashboard');
}
</script>

<style scoped>
.student-exam-list {
  --dashboard-scale: clamp(0.9, calc((100vw - 260px) / 1420), 1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  box-sizing: border-box;
  width: calc(100% / var(--dashboard-scale));
  transform: scale(var(--dashboard-scale));
  transform-origin: top left;
  background: transparent;
}
@supports (zoom: 1) {
  .student-exam-list {
    width: 100%;
    transform: none;
    zoom: var(--dashboard-scale);
  }
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-radius: 20px !important;
}
.header-left h2 {
  margin: 0;
  color: #0F172A;
  font-size: 24px;
}
.header-sub {
  margin-top: 6px;
  color: #64748B;
  font-size: 14px;
}
.list-card {
  padding: 16px;
  border-radius: 20px !important;
}
@media (max-width: 1200px) {
  .student-exam-list {
    --dashboard-scale: 1;
    padding: 20px;
    gap: 16px;
    width: 100%;
    transform: none;
  }
  @supports (zoom: 1) {
    .student-exam-list {
      zoom: 1;
    }
  }
}
</style>
