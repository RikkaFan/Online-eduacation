<template>
  <div class="admin-log-management page-shell">
    <div class="glass-card page-hero">
      <h2 class="hero-title">日志审计</h2>
      <p class="hero-subtitle">追踪系统关键操作轨迹，定位异常行为与审计事件。</p>
    </div>

    <el-card class="glass-card panel-card" shadow="never">
      <div class="panel-header">
        <h2>系统操作行为审计</h2>
      </div>

      <el-table :data="logs" v-loading="loading" style="width: 100%" empty-text="暂无操作日志">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="username" label="操作人" width="140">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operation" label="操作内容" min-width="240" />
        <el-table-column prop="method" label="请求方法" width="130">
          <template #default="{ row }">
            <el-tag :type="methodTagType(row.method)" effect="light">{{ row.method || 'N/A' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP 地址" width="170">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" min-width="200">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getLogs } from '@/api/log';

const loading = ref(false);
const logs = ref([]);

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function methodTagType(method) {
  const m = String(method || '').toUpperCase();
  if (m === 'DELETE') return 'danger';
  if (m === 'POST') return 'success';
  if (m === 'PUT' || m === 'PATCH') return 'warning';
  if (m === 'GET') return 'info';
  return '';
}

onMounted(async () => {
  loading.value = true;
  try {
    const data = await getLogs();
    logs.value = Array.isArray(data) ? data : [];
    if (logs.value.length === 0) {
      const retryData = await getLogs();
      logs.value = Array.isArray(retryData) ? retryData : [];
    }
  } catch (e) {
    ElMessage.error(e.message || '加载日志失败');
    logs.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page-shell {
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
  .page-shell {
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
.page-hero {
  padding: 24px;
  border-radius: 20px !important;
}
.hero-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1c1c1e;
}
.hero-subtitle {
  margin: 12px 0 0;
  font-size: 14px;
  color: #8E8E93;
}
.panel-card {
  border-radius: 20px !important;
}
.panel-card :deep(.el-card__body) {
  padding: 24px;
}
.panel-header {
  margin-bottom: 16px;
}
.panel-header h2 {
  margin: 0;
  font-size: 22px;
  color: #1c1c1e;
  font-weight: 700;
}
.panel-card :deep(.el-table) {
  --el-table-header-bg-color: rgba(246, 250, 255, 0.9);
  --el-table-row-hover-bg-color: rgba(236, 246, 255, 0.65);
  border-radius: 14px;
  overflow: hidden;
}
.panel-card :deep(.el-tag) {
  border-radius: 12px !important;
}
@media (max-width: 1200px) {
  .page-shell {
    --dashboard-scale: 1;
    padding: 28px 20px 20px;
    gap: 20px;
    width: 100%;
    transform: none;
  }
  @supports (zoom: 1) {
    .page-shell {
      zoom: 1;
    }
  }
}
@media (max-width: 760px) {
  .page-shell {
    padding: 20px 16px;
    gap: 16px;
  }
}
</style>
