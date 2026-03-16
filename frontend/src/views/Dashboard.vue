<template>
  <div class="dashboard-container">
    <el-container>
      <el-header class="dashboard-header">
        <span>在线考试系统</span>
        <el-button type="danger" @click="handleLogout">登出</el-button>
      </el-header>
      <el-main>
        <div v-if="isAdmin">
          <AdminView />
        </div>
        <div v-else-if="isTeacher">
          <TeacherView />
        </div>
        <div v-else-if="isStudent">
          <StudentView />
        </div>
        <div v-else>
          <p>未知的用户角色。</p>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/auth';
import AdminView from './AdminView.vue';
import TeacherView from './TeacherView.vue';
import StudentView from './StudentView.vue';

const authStore = useAuthStore();
const router = useRouter();

const isAdmin = computed(() => authStore.roles.includes('ROLE_ADMIN'));
const isTeacher = computed(() => authStore.roles.includes('ROLE_TEACHER'));
const isStudent = computed(() => authStore.roles.includes('ROLE_STUDENT'));

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #409EFF;
  color: white;
  padding: 0 20px;
}
</style>
