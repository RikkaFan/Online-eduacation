<template>
  <div class="admin-log-management">
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
  } catch (e) {
    ElMessage.error(e.message || '加载日志失败');
    logs.value = [];
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.admin-log-management {
  padding: 0;
}

.panel-card {
  padding: 6px 6px 2px;
}

.panel-header {
  margin-bottom: 12px;
}

.panel-header h2 {
  margin: 0;
  font-size: 24px;
  color: #0F172A;
}
</style>
