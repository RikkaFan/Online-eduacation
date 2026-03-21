<template>
  <el-container class="apple-layout">
    <el-aside width="auto" class="apple-sidebar glass-card elastic-sidebar">
      <div class="logo-area">
        <h2>管理控制台</h2>
      </div>
      <el-menu :default-active="$route.path" router class="apple-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Monitor /></el-icon>
          <span>日志审计</span>
        </el-menu-item>
      </el-menu>
      <div class="nav-bottom">
        <el-dropdown placement="right-end">
          <div class="user-profile">
            <el-avatar :size="28">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <span style="margin-left: 8px;">{{ userName }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openProfile">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-container>
      <el-main class="apple-main">
        <router-view />
      </el-main>
    </el-container>
    <UserProfileDialog v-model="profileDialogVisible" />
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { DataBoard, UserFilled, Monitor } from '@element-plus/icons-vue';
import UserProfileDialog from '@/views/UserProfileDialog.vue';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const profileDialogVisible = ref(false);
const userName = computed(() => user.value?.username || '管理员');

function openProfile() {
  profileDialogVisible.value = true;
}

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.apple-layout {
  height: 100vh;
  background-color: #F4F7FC;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
}

.apple-sidebar {
  background-color: #FFFFFF;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin: 12px;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05);
}

.logo-area {
  padding: 24px 0;
  margin-bottom: 16px;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-area h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1D1D1F;
  margin: 0;
}

.apple-menu {
  border-right: none !important;
  background: transparent;
}

::v-deep(.apple-menu .el-menu-item) {
  border-radius: 12px;
  margin-bottom: 4px;
  color: #334155;
  height: 48px;
  line-height: 48px;
}

::v-deep(.apple-menu .el-menu-item.is-active) {
  background-color: #E0E7FF;
  color: #4F46E5;
  font-weight: 600;
}

.apple-main {
  padding: 20px;
}

.nav-bottom {
  padding-top: 12px;
}

.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 8px 10px;
  color: #334155;
}
.user-profile :deep(.el-avatar) {
  flex-shrink: 0;
  background: linear-gradient(135deg, #93c5fd, #60a5fa);
  color: #fff;
}
</style>
