<template>
  <div class="admin-dashboard">
    <div class="glass-card welcome-banner">
      <div class="hero-left">
        <h2 class="hero-title">欢迎回来，{{ userName }}！</h2>
        <p class="hero-subtitle">这里是管理员全局视图，聚焦用户、课程与考试运行态势。</p>
      </div>
    </div>

    <div class="metrics-grid">
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><UserFilled /></el-icon>
        <div class="metric-label">系统总用户数</div>
        <div class="metric-value">{{ stats.totalUsers }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><Reading /></el-icon>
        <div class="metric-label">累计课程数</div>
        <div class="metric-value">{{ stats.totalCourses }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><FolderOpened /></el-icon>
        <div class="metric-label">题库总容量</div>
        <div class="metric-value">{{ stats.totalQuestions }}</div>
      </div>
      <div class="glass-card metric-card">
        <el-icon class="metric-icon"><EditPen /></el-icon>
        <div class="metric-label">累计考试场次</div>
        <div class="metric-value">{{ stats.totalExams }}</div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="glass-card trend-card">
        <div class="section-head">
          <h3>系统近期活跃趋势</h3>
        </div>
        <div class="trend-sub">服务器状态、业务活跃度与核心资源巡检</div>
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { EditPen, FolderOpened, Reading, UserFilled } from '@element-plus/icons-vue';
import { useAuthStore } from '@/store/auth';
import { getAdminStats } from '@/api/stats';

const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const stats = ref({
  totalUsers: 0,
  totalCourses: 0,
  totalQuestions: 0,
  totalExams: 0,
});
const userName = computed(() => user.value?.username || '管理员');

onMounted(async () => {
  try {
    const data = await getAdminStats();
    stats.value = {
      totalUsers: data?.totalUsers ?? 0,
      totalCourses: data?.totalCourses ?? 0,
      totalQuestions: data?.totalQuestions ?? 0,
      totalExams: data?.totalExams ?? 0,
    };
  } catch (e) {
    stats.value = {
      totalUsers: 0,
      totalCourses: 0,
      totalQuestions: 0,
      totalExams: 0,
    };
    ElMessage.error(e.message || '获取管理端统计失败');
  }
});
</script>

<style scoped>
.admin-dashboard {
  --dashboard-scale: clamp(0.9, calc((100vw - 260px) / 1420), 1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 40px 24px 24px;
  box-sizing: border-box;
  width: calc(100% / var(--dashboard-scale));
  transform: scale(var(--dashboard-scale));
  transform-origin: top left;
  background: transparent;
}
@supports (zoom: 1) {
  .admin-dashboard {
    width: 100%;
    transform: none;
    zoom: var(--dashboard-scale);
  }
}
.glass-card {
  border-radius: 16px !important;
  border: 1px solid rgba(212, 224, 244, 0.96) !important;
  background: rgba(255, 255, 255, 0.84) !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06), 0 2px 6px rgba(15, 23, 42, 0.03) !important;
}
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  position: relative;
  overflow: hidden;
  padding: 0 36px 0 108px;
  height: 164px;
  min-height: 164px;
  border-radius: 20px !important;
  isolation: isolate;
}
.welcome-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(248, 252, 255, 0.98) 0%, rgba(243, 249, 255, 0.94) 50%, rgba(243, 249, 255, 0.2) 72%),
    repeating-linear-gradient(90deg, rgba(148, 163, 184, 0.12) 0 1px, transparent 1px 52px);
  z-index: 0;
  pointer-events: none;
}
.welcome-banner::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 50%;
  background-image: url('../assets/library-banner.svg');
  background-size: cover;
  background-position: center center;
  mask-image: linear-gradient(to right, transparent, black 40%);
  -webkit-mask-image: linear-gradient(to right, transparent, black 40%);
  z-index: 0;
  pointer-events: none;
  opacity: 0.9;
}
.hero-left {
  flex: 1;
  max-width: min(56%, 660px);
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-top: 12px;
  position: relative;
  z-index: 2;
  min-width: 0;
}
.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #1c1c1e;
  margin: 0;
}
.hero-subtitle {
  margin-top: 14px;
  font-size: 14px;
  font-weight: 400;
  color: #8E8E93;
}
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.metric-card {
  padding: 18px 14px;
  min-height: 108px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  text-align: center;
  border-radius: 20px !important;
}
.metric-icon {
  color: #0a84ff;
  font-size: 28px;
}
.metric-label {
  font-size: 14px;
  color: #8E8E93;
  font-weight: 500;
}
.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: #1c1c1e;
  line-height: 1;
}
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 24px;
}
.trend-card {
  padding: 24px;
  height: 270px;
  display: flex;
  flex-direction: column;
  border-radius: 20px !important;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.section-head h3 {
  margin: 0;
  color: #1c1c1e;
  font-weight: 700;
}
.trend-sub {
  color: #8E8E93;
  font-size: 13px;
}
.trend-placeholder {
  margin-top: 14px;
  border-radius: 14px;
  border: 1px dashed rgba(148, 163, 184, 0.28);
  background: rgba(255, 255, 255, 0.65);
  padding: 16px;
  flex: 1;
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
@media (max-width: 1200px) {
  .admin-dashboard {
    --dashboard-scale: 1;
    padding: 28px 20px 20px;
    gap: 20px;
    width: 100%;
    transform: none;
  }
  @supports (zoom: 1) {
    .admin-dashboard {
      zoom: 1;
    }
  }
  .welcome-banner {
    padding: 0 24px;
    height: 136px;
    min-height: 136px;
    align-items: center;
  }
  .welcome-banner::after {
    width: 100%;
    height: 44%;
    top: auto;
    left: 0;
    bottom: 0;
    background-position: center bottom;
  }
  .hero-left {
    max-width: 100%;
  }
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 760px) {
  .admin-dashboard {
    padding: 20px 16px;
    gap: 16px;
  }
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .hero-title {
    font-size: 28px;
  }
}
</style>
