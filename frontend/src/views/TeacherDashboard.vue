<template>
  <div class="teacher-dashboard">
    <header class="dashboard-header">
      <h1>教师仪表盘</h1>
    </header>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6" :xs="12">
        <el-card shadow="never" class="apple-card stat-card">
          <el-statistic title="总课程数" :value="adminStats.totalCourses" />
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never" class="apple-card stat-card">
          <el-statistic title="考试场次" :value="adminStats.totalExams" />
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never" class="apple-card stat-card">
          <el-statistic title="题库总量" :value="adminStats.totalQuestions" />
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never" class="apple-card stat-card">
          <el-statistic title="系统用户" :value="adminStats.totalUsers" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="module-cards">
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature-card" @click="goTo('/teacher/courses')">
          <div class="card-content">
            <h3>课程管理</h3>
            <p>管理您的课程信息和内容</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature-card" @click="goTo('/teacher/questions')">
          <div class="card-content">
            <h3>题库管理</h3>
            <p>维护和组织题目资源</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature-card" @click="goTo('/teacher/exams')">
          <div class="card-content">
            <h3>考试管理</h3>
            <p>发布和批改在线考试</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getAdminStats } from '@/api/stats';

const router = useRouter();
const adminStats = ref({
  totalCourses: 0,
  totalExams: 0,
  totalQuestions: 0,
  totalUsers: 0
});

onMounted(async () => {
  try {
    const data = await getAdminStats();
    adminStats.value = { ...adminStats.value, ...(data || {}) };
  } catch (e) {
    ElMessage.error(e.message || '加载统计数据失败');
  }
});

function goTo(path) {
  router.push(path);
}
</script>

<style scoped>
.teacher-dashboard {
  padding: 0;
}

.dashboard-header {
  margin-bottom: 24px;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
  color: #2D3B45;
}

.stats-row {
  margin-bottom: 16px;
}

.apple-card {
  border: none;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.06);
}

.stat-card {
  text-align: center;
}

.module-cards {
  margin-top: 8px;
}

.feature-card {
  cursor: pointer;
  transition: all 0.2s ease;
  height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.feature-card:hover {
  transform: translateY(-3px);
}

.card-content {
  text-align: center;
}

.card-content h3 {
  margin: 0 0 10px;
  color: #303133;
  font-size: 18px;
}

.card-content p {
  color: #606266;
  margin: 0;
  font-size: 14px;
}
</style>
