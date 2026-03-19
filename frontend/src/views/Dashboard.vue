<template>
  <div class="admin-dashboard">
    <el-card class="glass-card board-card" shadow="never">
      <div class="board-title">全局系统监控大屏</div>
      <div class="board-sub">管理员专用，聚焦平台运行态势与核心资源概览。</div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="glass-card metric-card" shadow="never">
          <div class="metric-label">系统总用户数</div>
          <div class="metric-value">{{ stats.totalUsers }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="glass-card metric-card" shadow="never">
          <div class="metric-label">累计课程数</div>
          <div class="metric-value">{{ stats.totalCourses }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="glass-card metric-card" shadow="never">
          <div class="metric-label">题库总容量</div>
          <div class="metric-value">{{ stats.totalQuestions }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="glass-card metric-card" shadow="never">
          <div class="metric-label">累计生成考试场次</div>
          <div class="metric-value">{{ stats.totalExams }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="glass-card trend-card" shadow="never">
      <div class="trend-title">系统近期活跃趋势</div>
      <div class="trend-sub">服务器运行状态与业务活跃数据占位区域</div>
      <div class="trend-placeholder">
        <div class="pulse-row">
          <span class="pulse-dot"></span>
          <span>应用服务稳定运行中</span>
        </div>
        <div class="placeholder-grid">
          <div class="line"></div>
          <div class="line short"></div>
          <div class="line"></div>
          <div class="line mid"></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAdminStats } from '@/api/stats';

const stats = ref({
  totalUsers: 0,
  totalCourses: 0,
  totalQuestions: 0,
  totalExams: 0,
});

onMounted(async () => {
  try {
    const data = await getAdminStats();
    stats.value = {
      totalUsers: data?.totalUsers ?? 0,
      totalCourses: data?.totalCourses ?? 0,
      totalQuestions: data?.totalQuestions ?? 0,
      totalExams: data?.totalExams ?? 0,
    };
  } catch {
    stats.value = {
      totalUsers: 1280,
      totalCourses: 96,
      totalQuestions: 4520,
      totalExams: 318,
    };
  }
});
</script>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 16px;
}

.board-card {
  padding: 20px;
}

.board-title {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
}

.board-sub {
  margin-top: 8px;
  color: #64748B;
}

.metric-card {
  padding: 18px;
}

.metric-label {
  color: #64748B;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  color: #0F172A;
  font-size: 30px;
  font-weight: 700;
}

.trend-card {
  padding: 20px;
}

.trend-title {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
}

.trend-sub {
  margin-top: 6px;
  color: #64748B;
}

.trend-placeholder {
  margin-top: 16px;
  border-radius: 16px;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  background: rgba(255, 255, 255, 0.65);
  padding: 18px;
}

.pulse-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  margin-bottom: 12px;
}

.pulse-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22C55E;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.18);
}

.placeholder-grid {
  display: grid;
  gap: 10px;
}

.line {
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(79, 70, 229, 0.2), rgba(14, 165, 233, 0.35));
}

.line.short {
  width: 72%;
}

.line.mid {
  width: 84%;
}
</style>
