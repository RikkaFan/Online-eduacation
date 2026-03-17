<template>
  <div class="teacher-view">
    <div class="header">
      <h1>教师面板</h1>
      <p>欢迎，教师！您可以在这里管理课程和考试。</p>
    </div>

    <el-row :gutter="20" class="module-cards">
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature-card" @click="goTo('/courses')">
          <div class="card-content">
            <h3>课程管理</h3>
            <p>管理您的课程信息和内容</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature-card" @click="goTo('/questions')">
          <div class="card-content">
            <h3>题库管理</h3>
            <p>维护和组织题目资源</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature-card" @click="goTo('/exams')">
          <div class="card-content">
            <h3>考试管理</h3>
            <p>发布和批改在线考试</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.teacher-view {
  padding: 20px;
}

.header {
  margin-bottom: 30px;
}

.module-cards {
  margin-top: 20px;
}

.feature-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.feature-card:hover {
  transform: translateY(-5px);
  background-color: #f5f7fa;
}

.card-content {
  text-align: center;
}

.card-content h3 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #303133;
  font-size: 1.2rem;
}

.card-content p {
  color: #606266;
  margin: 0;
  font-size: 0.9rem;
}
</style>
