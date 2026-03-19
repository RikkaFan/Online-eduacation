<template>
  <el-container class="apple-layout">
    <el-aside width="240px" class="apple-sidebar glass-card">
      <div class="logo-area">
        <h2>管理控制台</h2>
      </div>
      <el-menu :default-active="$route.path" router class="apple-menu">
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
            <el-avatar size="small" />
            <span style="margin-left: 8px;">管理员</span>
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
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { UserFilled, Monitor } from '@element-plus/icons-vue';
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
</style>
