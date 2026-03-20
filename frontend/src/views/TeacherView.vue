<template>
  <el-container class="apple-layout">
    <el-aside width="240px" class="apple-sidebar glass-card">
      <div class="logo-area">
        <h2>在线测评系统</h2>
      </div>
      <el-menu :default-active="$route.path" router class="apple-menu">
        <el-menu-item index="/teacher/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/teacher/courses">
          <el-icon><Reading /></el-icon>
          <span>课程管理</span>
        </el-menu-item>
        <el-menu-item index="/teacher/questions">
          <el-icon><Folder /></el-icon>
          <span>题库管理</span>
        </el-menu-item>
        <el-menu-item index="/teacher/exams">
          <el-icon><EditPen /></el-icon>
          <span>考试管理</span>
        </el-menu-item>
        <el-menu-item index="/teacher/scores">
          <el-icon><DataLine /></el-icon>
          <span>成绩分析</span>
        </el-menu-item>
        <el-menu-item index="/teacher/grading">
          <el-icon><DocumentChecked /></el-icon>
          <span>阅卷管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users" v-if="isAdmin">
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </el-menu-item>
      </el-menu>
      <div class="nav-bottom">
        <el-dropdown placement="right-end">
          <div class="user-profile">
            <el-avatar size="small" />
            <span style="margin-left: 8px;">账户</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-container>
      <el-header class="apple-header">
        <div class="header-left">欢迎回来！</div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-profile">
              <el-avatar size="small" />
              <span style="margin-left: 8px;">{{ roleLabel }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="apple-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  <UserProfileDialog v-model="profileDialogVisible" />
</template>

<script setup>
import { computed, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { DataBoard, Reading, Folder, EditPen, DataLine, DocumentChecked, Setting } from '@element-plus/icons-vue';
import UserProfileDialog from '@/views/UserProfileDialog.vue';

const router = useRouter();
const authStore = useAuthStore();
const { roles } = storeToRefs(authStore);
const profileDialogVisible = ref(false);

const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'));
const roleLabel = computed(() => (isAdmin.value ? '管理员' : '教师'));

function goProfile() {
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
::v-deep(.apple-menu .el-menu-item:hover) {
  background-color: rgba(79, 70, 229, 0.08);
}
.apple-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  height: 60px;
  border-radius: 20px;
  margin: 12px 12px 0 0;
  padding: 0 16px;
}
.apple-main {
  padding: 20px;
}
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #1D1D1F;
  font-weight: 500;
}
.nav-bottom {
  width: 100%;
  margin-bottom: 16px;
}
:deep(.el-tooltip__trigger:focus-visible) {
  outline: none;
}
</style>
