<template>
  <div class="lms-dashboard">
    <header class="dashboard-header">
      <h1>仪表盘</h1>
      <el-button type="primary" plain size="small">管理课程</el-button>
    </header>
    <el-row :gutter="24" class="course-grid">
      <el-col :span="6" v-for="(color, index) in colors" :key="index" style="margin-bottom: 24px;">
        <el-card class="lms-course-card" :body-style="{ padding: '0px' }" shadow="hover">
          <div class="card-color-header" :style="{ backgroundColor: color }">
            <el-icon class="card-more-icon"><MoreFilled /></el-icon>
          </div>
          <div class="card-content">
            <h3 class="course-code">CS2026</h3>
            <h2 class="course-name" :style="{ color: color }">Spring Boot 高级企业级开发</h2>
            <p class="course-term">2025-2026 秋季学期</p>
          </div>
          <div class="card-footer">
            <el-icon title="公告"><Notification /></el-icon>
            <el-icon title="作业"><Document /></el-icon>
            <el-icon title="讨论"><ChatDotSquare /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { MoreFilled, Notification, Document, ChatDotSquare } from '@element-plus/icons-vue';
const colors = ['#34495E', '#16A085', '#2980B9', '#8E44AD'];
</script>

<style>
.lms-dashboard {
  background-color: transparent;
  margin: 0;
  padding: 0;
}
.dashboard-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #C7CDD1; padding-bottom: 16px; margin-bottom: 24px; }
.dashboard-header h1 { margin: 0; font-size: 24px; font-weight: 400; color: #2D3B45; }
.course-grid { margin: 0 !important; }
.lms-course-card { border-radius: 4px; border: 1px solid #C7CDD1; transition: box-shadow 0.2s; cursor: pointer; background: white; }
.lms-course-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.card-color-header { height: 140px; position: relative; }
.card-more-icon { position: absolute; top: 12px; right: 12px; color: white; font-size: 20px; cursor: pointer; }
.card-content { padding: 16px; height: 110px; }
.course-code { margin: 0 0 4px 0; font-size: 12px; color: #556572; font-weight: normal; }
.course-name { margin: 0 0 8px 0; font-size: 16px; font-weight: 700; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.course-term { margin: 0; font-size: 12px; color: #556572; }
.card-footer { padding: 12px 16px; border-top: 1px solid #E7E9EB; display: flex; gap: 16px; color: #556572; font-size: 18px; }
.card-footer .el-icon:hover { color: #0076D3; }
</style>
