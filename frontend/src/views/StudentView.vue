<template>
  <el-container class="apple-layout">
    <el-aside width="auto" class="glass-card elastic-sidebar apple-sidebar">
      <div class="profile-card">
        <el-avatar :size="42">
          <el-icon><UserFilled /></el-icon>
        </el-avatar>
        <div class="profile-meta">
          <div class="profile-name">{{ userName }}</div>
          <div class="profile-sub">加油，继续保持学习节奏</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="apple-menu">
        <el-menu-item index="/student/dashboard">
          <el-icon><House /></el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/student/exams">
          <el-icon><Position /></el-icon>
          <span>考试列表</span>
        </el-menu-item>
        <el-menu-item index="/student/scores">
          <el-icon><DataAnalysis /></el-icon>
          <span>成绩查询</span>
        </el-menu-item>
        <el-menu-item index="/student/practice">
          <el-icon><Warning /></el-icon>
          <span>错题本</span>
        </el-menu-item>
        <el-menu-item index="/student/favorites">
          <el-icon><Reading /></el-icon>
          <span>我的收藏</span>
        </el-menu-item>
        <el-menu-item index="/student/smart-practice">
          <el-icon><EditPen /></el-icon>
          <span>自主刷题</span>
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
              <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-main style="padding: 0; background: transparent; overflow-y: auto; overflow-x: hidden;">
      <router-view />
    </el-main>
  </el-container>
  <UserProfileDialog v-model="profileDialogVisible" />
</template>

<script setup>
import { computed, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import { DataAnalysis, EditPen, House, Position, Reading, UserFilled, Warning } from '@element-plus/icons-vue';
import UserProfileDialog from '@/views/UserProfileDialog.vue';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const profileDialogVisible = ref(false);
const userName = computed(() => user.value?.username || '同学');

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
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px 16px;
  margin-top: clamp(62px, 8vh, 82px);
}
.profile-meta {
  min-width: 0;
}
.profile-card :deep(.el-avatar) {
  flex-shrink: 0;
}
.user-profile :deep(.el-avatar) {
  flex-shrink: 0;
  background: linear-gradient(135deg, #93c5fd, #60a5fa);
  color: #fff;
}
.profile-name {
  color: #1d1d1f;
  font-size: 15px;
  font-weight: 700;
}
.profile-sub {
  color: #64748b;
  font-size: 12px;
  margin-top: 2px;
}
.apple-menu {
  border-right: none !important;
  background: transparent;
}
::v-deep(.apple-menu .el-menu-item) {
  border-radius: 12px;
  margin-bottom: 4px;
  color: #475569;
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
.nav-bottom {
  width: 100%;
  margin-bottom: 16px;
}
@media (max-width: 1200px) {
  .profile-card {
    margin-top: 28px;
  }
}
</style>
