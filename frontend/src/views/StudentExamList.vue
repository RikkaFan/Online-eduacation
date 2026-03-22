<template>
  <div class="student-exam-list">
    <div class="glass-card page-hero">
      <div class="hero-left">
        <h2>考试列表</h2>
        <div class="hero-sub">仅展示你已选课程的考试安排、状态与进入入口</div>
      </div>
    </div>
    <div class="glass-card list-card">
      <div class="list-head-row">
        <span>考试名称</span>
        <span>所属课程</span>
        <span>日期</span>
        <span>倒计时</span>
        <span>状态</span>
        <span>入口</span>
      </div>
      <div v-loading="loading" class="list-scroll">
        <template v-if="exams.length">
          <div v-for="row in exams" :key="row.id" class="exam-row">
            <div class="row-cell title-cell">
              <el-icon class="exam-cal-icon"><Calendar /></el-icon>
              <span class="exam-title">{{ row.title || '未命名考试' }}</span>
            </div>
            <div class="row-cell">{{ row.course?.courseName || row.courseName || '-' }}</div>
            <div class="row-cell">{{ formatDateTime(row.startTime) }}</div>
            <div class="row-cell">{{ formatCountdown(row) }}</div>
            <div class="row-cell status-cell">
              <el-tag effect="plain" :type="statusType(getStatus(row))">{{ getStatusText(getStatus(row)) }}</el-tag>
            </div>
            <div class="row-cell action-cell">
              <el-button type="primary" size="small" plain round :disabled="getStatus(row) !== 'ongoing'" @click="enterExam(row.id)">
                {{ getStatus(row) === 'finished' ? '已结束' : getStatus(row) === 'pending' ? '未开始' : '进入考试' }}
              </el-button>
            </div>
          </div>
        </template>
        <el-empty
          v-else
          :description="hasEnrolledCourse ? '已选课程暂无考试' : '请先到“我的课程”选课后查看考试'"
          :image-size="84"
        >
          <el-button v-if="!hasEnrolledCourse" type="primary" round @click="goCourses">前往我的课程</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Calendar } from '@element-plus/icons-vue';
import { getEnrolledExams } from '@/api/examTaking';
import { getMyEnrolledCourses } from '@/api/course';
import { getMyScores } from '@/api/score';

const router = useRouter();
const exams = ref([]);
const loading = ref(false);
const hasEnrolledCourse = ref(true);

onMounted(async () => {
  loading.value = true;
  try {
    const [allExams, myScores, enrolledCourses] = await Promise.all([
      getEnrolledExams(),
      getMyScores().catch(() => []),
      getMyEnrolledCourses().catch(() => []),
    ]);
    hasEnrolledCourse.value = Array.isArray(enrolledCourses) && enrolledCourses.length > 0;
    const submittedExamIds = new Set((myScores || []).map(s => s?.exam?.id).filter(Boolean));
    exams.value = (allExams || []).sort((a, b) => new Date(a.startTime || 0).getTime() - new Date(b.startTime || 0).getTime()).map(exam => ({
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
function formatCountdown(exam) {
  const status = getStatus(exam);
  if (status === 'finished') return '已结束';
  const now = Date.now();
  const target = status === 'pending'
    ? new Date(exam.startTime || 0).getTime()
    : new Date(exam.endTime || 0).getTime();
  if (!target || Number.isNaN(target)) return '-';
  const diff = target - now;
  if (diff <= 0) return status === 'pending' ? '即将开始' : '即将结束';
  const totalMinutes = Math.floor(diff / 60000);
  const days = Math.floor(totalMinutes / (60 * 24));
  const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
  const minutes = totalMinutes % 60;
  if (days > 0) return `${days}天${hours}时`;
  if (hours > 0) return `${hours}时${minutes}分`;
  return `${minutes}分钟`;
}
function enterExam(id) {
  router.push(`/student/exam-ready/${id}`);
}
function goCourses() {
  router.push('/student/courses');
}
</script>

<style scoped>
.student-exam-list {
  --glass-border: rgba(212, 224, 244, 0.95);
  --glass-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03);
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 8px 4px 0;
}
.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 126px;
  padding: 0 34px;
  border-radius: 22px !important;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(18px) saturate(170%);
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
  font-size: 24px;
}
.hero-sub {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}
.list-card {
  border-radius: 22px !important;
  padding: 20px;
  min-height: 456px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(16px) saturate(170%);
  background: rgba(255, 255, 255, 0.84);
}
.list-head-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
  align-items: center;
  text-align: center;
  padding: 0 10px 14px;
  color: #8e8e93;
  font-size: 12px;
  font-weight: 600;
}
.list-scroll {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-right: 4px;
}
.exam-row {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(212, 224, 244, 0.92);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.exam-row:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.1);
}
.row-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.exam-cal-icon {
  font-size: 18px;
  color: #007AFF;
  margin-right: 8px;
}
.title-cell .exam-title {
  color: #0f172a;
  font-weight: 600;
}
.title-cell {
  justify-content: flex-start;
  text-align: left;
}
.status-cell {
  justify-content: center;
}
.action-cell {
  justify-content: center;
}
@media (max-width: 1200px) {
  .list-head-row,
  .exam-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .page-hero {
    min-height: 104px;
    padding: 0 20px;
  }
}
</style>
