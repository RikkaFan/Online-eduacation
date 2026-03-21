<template>
  <el-container class="apple-layout">
    <el-aside width="auto" class="apple-sidebar glass-card elastic-sidebar">
      <div class="logo-area">
        <h2>在线测评系统</h2>
      </div>
      <div class="menu-wrap">
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
      </div>
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
              <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-container>
      <el-main style="padding: 0; background: transparent; overflow-y: auto; overflow-x: hidden;">
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
import { DataBoard, Reading, Folder, EditPen, DataLine, DocumentChecked, Setting, UserFilled } from '@element-plus/icons-vue';
import UserProfileDialog from '@/views/UserProfileDialog.vue';

const router = useRouter();
const authStore = useAuthStore();
const { roles, user } = storeToRefs(authStore);
const profileDialogVisible = ref(false);

const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'));
const userName = computed(() => user.value?.username || '用户');

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
  display: flex;
  height: 100vh;
  overflow: hidden;
  padding: 16px;
  gap: 14px;
  box-sizing: border-box;
  background-color: transparent;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
}
.apple-sidebar {
  width: clamp(174px, 12.2vw, 218px) !important;
  padding: 16px 12px;
  display: grid;
  grid-template-rows: auto 1fr auto;
}
.logo-area {
  padding: 8px 6px 10px;
  color: #1d1d1f;
  position: relative;
  top: 18px;
  text-align: center;
}
.logo-area h2 {
  font-size: 17px;
  font-weight: 700;
  color: #1d1d1f;
  letter-spacing: 0.02em;
  margin: 0;
}
.menu-wrap {
  display: flex;
  align-items: center;
}
.menu-wrap .apple-menu {
  width: 100%;
}
.apple-menu {
  border-right: none !important;
  background: transparent;
}
::v-deep(.apple-menu .el-menu-item) {
  border-radius: 12px;
  margin-bottom: 4px;
  color: #334155;
  height: 46px;
  line-height: 46px;
}
::v-deep(.apple-menu .el-menu-item.is-active) {
  background-color: #E0E7FF;
  color: #4F46E5;
  font-weight: 600;
}
::v-deep(.apple-menu .el-menu-item:hover) {
  background-color: rgba(79, 70, 229, 0.08);
}
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #1D1D1F;
  font-weight: 500;
}
.user-profile :deep(.el-avatar) {
  flex-shrink: 0;
  background: linear-gradient(135deg, #93c5fd, #60a5fa);
  color: #fff;
}
.nav-bottom {
  width: 100%;
  margin-bottom: 16px;
}
:deep(.el-tooltip__trigger:focus-visible) {
  outline: none;
}
@media (max-width: 1200px) {
  .logo-area {
    top: 10px;
  }
  .logo-area h2 {
    font-size: 16px;
  }
}
</style>
