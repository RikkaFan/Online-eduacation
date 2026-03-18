<template>
  <el-container class="apple-layout">
    <el-aside width="240px" class="apple-sidebar">
      <div class="logo-area">
        <h2>在线测评系统</h2>
      </div>
      <el-menu :default-active="$route.path" router class="apple-menu">
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>控制台首页</span>
        </el-menu-item>
        <el-menu-item index="/student/exams">
          <el-icon><HomeFilled /></el-icon>
          <span>考试列表</span>
        </el-menu-item>
        <el-menu-item index="/student/scores">
          <el-icon><HomeFilled /></el-icon>
          <span>我的成绩</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="apple-header">
        <div class="header-left">欢迎回来！</div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-profile">
              <el-avatar size="small" />
              <span style="margin-left: 8px;">学生</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="apple-main">
        <!-- 欢迎 Banner -->
        <el-card class="apple-card banner-card" shadow="never">
          <div class="banner-content">
            <div class="banner-title">你好，学生</div>
            <div class="banner-sub">欢迎来到在线教育测评系统</div>
          </div>
        </el-card>

        <!-- 统计卡片 -->
        <el-row :gutter="24" class="stats-row">
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">课程数</div>
              <div class="stat-number">{{ courseCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">待办考试</div>
              <div class="stat-number">{{ upcomingExams.length }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">全部考试</div>
              <div class="stat-number">{{ examsCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="apple-card stat-card" shadow="never">
              <div class="stat-title">近期课程</div>
              <div class="stat-number">{{ recentCourses.length }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 网格卡片：左近期课程 + 右待办考试 -->
        <el-row :gutter="24">
          <el-col :span="16">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">近期课程</div>
                <el-button link type="primary" @click="goCourses">查看全部</el-button>
              </div>
              <div v-if="recentCourses.length === 0" class="empty">暂无课程</div>
              <div v-else class="horizontal-list">
                <div v-for="c in recentCourses" :key="c.id" class="apple-card item-card">
                  <div class="item-title">{{ c.courseName }}</div>
                  <div class="item-sub">教师ID：{{ c.teacherId || '-' }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">近期考试</div>
                <el-button link type="primary" @click="goExams">进入列表</el-button>
              </div>
              <div v-if="upcomingExams.length === 0" class="empty">暂无待办考试</div>
              <ul v-else class="exam-list">
                <li v-for="e in upcomingExams" :key="e.id" class="exam-item">
                  <div class="exam-title">{{ e.title }}</div>
                  <div class="exam-sub">截止：{{ formatDateTime(e.endTime) }}</div>
                  <el-button link type="primary" @click="enterExam(e.id)">进入考试</el-button>
                </li>
              </ul>
            </el-card>
          </el-col>
        </el-row>

        <!-- 下方内容：近期成绩 + 快速入口 -->
        <el-row :gutter="24">
          <el-col :span="16">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-header">
                <div class="card-title">我的近期成绩</div>
                <el-button link type="primary" @click="goScores">查看全部</el-button>
              </div>
              <div v-if="recentScores.length === 0" class="empty">暂无成绩记录</div>
              <ul v-else class="score-list">
                <li v-for="s in recentScores" :key="s.id" class="score-item">
                  <div class="score-left">
                    <div class="score-title">{{ s.exam?.title || '未命名考试' }}</div>
                    <div class="score-sub">得分：{{ s.score ?? '-' }}</div>
                  </div>
                  <div class="score-right">{{ formatDateTime(s.submittedAt || s.createdAt) }}</div>
                </li>
              </ul>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="apple-card list-card" shadow="never">
              <div class="card-title" style="margin-bottom: 12px;">快速入口</div>
              <div class="quick-actions">
                <el-button type="primary" plain @click="goExams">进入考试列表</el-button>
                <el-button type="primary" plain @click="goScores">查看我的成绩</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { DataBoard, Reading, EditPen, DataLine } from '@element-plus/icons-vue';
</script>

<style scoped>
/* Layout 基础 */
.apple-layout {
  height: 100vh;
  background-color: #F5F5F7;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
}
.apple-sidebar {
  background-color: transparent;
  padding: 16px 12px;
}
.logo-area {
  padding: 12px 16px;
  margin-bottom: 16px;
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
  color: #515154;
  height: 48px;
  line-height: 48px;
}
::v-deep(.apple-menu .el-menu-item.is-active) {
  background-color: #E8F0FE;
  color: #1967D2;
  font-weight: 600;
}
::v-deep(.apple-menu .el-menu-item:hover) {
  background-color: rgba(0,0,0,0.04);
}
.apple-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  height: 60px;
}
.apple-main {
  padding: 24px;
}
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #1D1D1F;
  font-weight: 500;
}

/* Apple Card 通用类（也在全局定义，局部保留以确保生效） */
.apple-card {
  background: #FFFFFF;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 24px;
  border: none;
}

/* Banner 与内容 */
.banner-card {
  margin-bottom: 12px;
  background: #fff;
}
.banner-content {
  padding: 40px 30px;
  background: linear-gradient(135deg, #fdfbfb 0%, #ebedee 100%);
}
.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: #1D1D1F;
}
.banner-sub {
  margin-top: 4px;
  color: #86868B;
}
.stats-row { margin-bottom: 16px; }
.stat-card { padding: 20px; }
.stat-title { color: #86868B; font-size: 14px; }
.stat-number { color: #1D1D1F; font-size: 32px; font-weight: 700; }
.list-card {
  border: none;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-title {
  font-weight: 600;
  color: #1D1D1F;
}
.empty {
  color: #86868B;
  padding: 12px 0;
  text-align: center;
}
.horizontal-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}
.item-card {
  min-width: 220px;
  padding: 16px;
}
.item-title { font-weight: 600; color: #1D1D1F; margin-bottom: 6px; }
.item-sub { color: #86868B; }
.exam-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.exam-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.exam-title { font-weight: 600; color: #1D1D1F; }
.exam-sub { color: #86868B; margin-right: 8px; }
.score-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.score-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.score-title { font-weight: 600; color: #1D1D1F; }
.score-sub { color: #86868B; }
.quick-actions { display: flex; gap: 10px; }
</style>
