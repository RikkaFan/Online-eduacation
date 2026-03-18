<template>
  <div class="admin-view">
    <div class="header">
      <h1>管理员面板</h1>
      <el-dropdown>
        <span class="user-profile">
          <el-avatar size="small" />
          <span style="margin-left: 8px;">管理员</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="openProfile">个人中心</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <p>欢迎，管理员！这里是管理控制台。</p>
    <UserProfileDialog v-model="profileDialogVisible" />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import UserProfileDialog from '@/views/UserProfileDialog.vue';

const router = useRouter();
const authStore = useAuthStore();
const profileDialogVisible = ref(false);

function openProfile() {
  profileDialogVisible.value = true;
}

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.admin-view {
  background: transparent;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>
