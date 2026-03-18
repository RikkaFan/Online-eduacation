<template>
  <div>
    <header class="dashboard-header">
      <h1>我的课程</h1>
    </header>
    <el-row :gutter="24">
      <el-col :span="6" v-for="c in courses" :key="c.id" style="margin-bottom: 24px;">
        <el-card class="lms-course-card" shadow="hover">
          <div class="card-content">
            <h3 class="course-code">COURSE #{{ c.id }}</h3>
            <h2 class="course-name">{{ c.courseName }}</h2>
            <p class="course-term">教师ID：{{ c.teacherId || '-' }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getCourses } from '@/api/course';

const courses = ref([]);
onMounted(async () => {
  try {
    courses.value = await getCourses();
  } catch (e) {
    courses.value = [];
  }
});
</script>

<style>
.dashboard-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #C7CDD1; padding-bottom: 16px; margin-bottom: 24px; }
.dashboard-header h1 { margin: 0; font-size: 24px; font-weight: 400; color: #2D3B45; }
.lms-course-card { border-radius: 4px; border: 1px solid #C7CDD1; transition: box-shadow 0.2s; cursor: pointer; background: white; }
.lms-course-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.card-content { padding: 16px; }
.course-code { margin: 0 0 4px 0; font-size: 12px; color: #556572; font-weight: normal; }
.course-name { margin: 0 0 8px 0; font-size: 16px; font-weight: 700; }
.course-term { margin: 0; font-size: 12px; color: #556572; }
</style>
