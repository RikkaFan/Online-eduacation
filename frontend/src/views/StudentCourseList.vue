<template>
  <div class="student-course-list">
    <header class="dashboard-header">
      <h1>我的课程</h1>
    </header>
    <el-row :gutter="24">
      <el-col :span="6" v-for="c in courses" :key="c.id" style="margin-bottom: 24px;">
        <el-card class="lms-course-card glass-card" shadow="hover">
          <div class="card-content">
            <h3 class="course-code">COURSE #{{ c.id }}</h3>
            <h2 class="course-name">{{ c.courseName }}</h2>
            <p class="course-term">教师ID：{{ c.teacherId || '-' }}</p>
            <div class="progress-wrap">
              <el-progress
                :percentage="Number(c.progressData?.percentage || 0)"
                :status="Number(c.progressData?.percentage || 0) >= 100 ? 'success' : ''"
                :stroke-width="12"
              />
              <div class="progress-text">已完成 {{ c.progressData?.completed || 0 }} / {{ c.progressData?.total || 0 }} 课时</div>
            </div>
            <div class="card-actions">
              <el-button type="primary" round @click="openLearningDrawer(c)">
                {{ Number(c.progressData?.percentage || 0) > 0 ? '继续学习' : '开始学习' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer
      v-model="learningDrawerVisible"
      :title="`课程学习：${activeCourse?.courseName || ''}`"
      direction="rtl"
      size="62%"
      class="learning-drawer"
    >
      <div v-if="chapterLoading" class="drawer-empty">正在加载课时内容...</div>
      <div v-else-if="activeChapters.length === 0" class="drawer-empty">当前课程暂未发布课时</div>
      <div v-else class="chapter-grid">
        <div v-for="(chapter, index) in activeChapters" :key="chapter.id" class="chapter-card glass-card">
          <div class="chapter-left">
            <div class="chapter-title">第 {{ index + 1 }} 节：{{ chapter.title || '未命名课时' }}</div>
            <div class="chapter-content">{{ chapter.content || '暂无课时内容' }}</div>
          </div>
          <div class="chapter-right">
            <el-tag v-if="isChapterCompleted(chapter.id)" type="success" effect="light" class="done-tag">
              <el-icon><Select /></el-icon>已完成
            </el-tag>
            <el-button
              v-else
              type="primary"
              :loading="completingChapterId === chapter.id"
              @click="handleCompleteChapter(chapter)"
            >
              标记已读 / 打卡
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Select } from '@element-plus/icons-vue';
import { getCourses } from '@/api/course';
import { getChapters, completeChapter, getCourseProgress } from '@/api/chapter';

const courses = ref([]);
const learningDrawerVisible = ref(false);
const chapterLoading = ref(false);
const activeCourse = ref(null);
const activeChapters = ref([]);
const completingChapterId = ref(null);
const completedChapterIds = ref(new Set());

function progressDefaults() {
  return { total: 0, completed: 0, percentage: 0 };
}

async function hydrateProgress(list) {
  const tasks = (list || []).map(async (course) => {
    try {
      const progress = await getCourseProgress(course.id);
      return { ...course, progressData: { ...progressDefaults(), ...(progress || {}) } };
    } catch {
      return { ...course, progressData: progressDefaults() };
    }
  });
  return Promise.all(tasks);
}

onMounted(async () => {
  try {
    const data = await getCourses();
    const courseList = Array.isArray(data) ? data : [];
    courses.value = await hydrateProgress(courseList);
  } catch (e) {
    courses.value = [];
    ElMessage.error(e.message || '加载课程失败');
  }
});

function isChapterCompleted(chapterId) {
  return completedChapterIds.value.has(chapterId);
}

async function refreshCourseProgress(courseId) {
  const idx = courses.value.findIndex(c => c.id === courseId);
  if (idx < 0) return;
  try {
    const progress = await getCourseProgress(courseId);
    courses.value[idx] = {
      ...courses.value[idx],
      progressData: { ...progressDefaults(), ...(progress || {}) },
    };
  } catch {
    courses.value[idx] = {
      ...courses.value[idx],
      progressData: progressDefaults(),
    };
  }
}

async function openLearningDrawer(course) {
  activeCourse.value = course;
  learningDrawerVisible.value = true;
  chapterLoading.value = true;
  activeChapters.value = [];
  completedChapterIds.value = new Set();
  try {
    const chapters = await getChapters(course.id);
    activeChapters.value = Array.isArray(chapters) ? chapters : [];
  } catch (e) {
    ElMessage.error(e.message || '加载课时失败');
  } finally {
    chapterLoading.value = false;
  }
}

async function handleCompleteChapter(chapter) {
  if (!activeCourse.value?.id) return;
  completingChapterId.value = chapter.id;
  try {
    await completeChapter(chapter.id);
    completedChapterIds.value.add(chapter.id);
    completedChapterIds.value = new Set(completedChapterIds.value);
    await refreshCourseProgress(activeCourse.value.id);
    ElMessage.success('打卡成功');
  } catch (e) {
    ElMessage.error(e.message || '打卡失败');
  } finally {
    completingChapterId.value = null;
  }
}
</script>

<style>
.student-course-list {
  padding: 0 4px;
}
.dashboard-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(226, 232, 240, 0.8); padding-bottom: 16px; margin-bottom: 24px; }
.dashboard-header h1 { margin: 0; font-size: 24px; font-weight: 400; color: #1E293B; }
.lms-course-card { border-radius: 16px; border: 1px solid rgba(226, 232, 240, 0.8); transition: box-shadow 0.2s; cursor: pointer; background: white; height: 100%; }
.lms-course-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.card-content { padding: 16px; display: grid; gap: 10px; }
.course-code { margin: 0 0 4px 0; font-size: 12px; color: #64748B; font-weight: normal; }
.course-name { margin: 0 0 8px 0; font-size: 16px; font-weight: 700; }
.course-term { margin: 0; font-size: 12px; color: #64748B; }
.progress-wrap { margin-top: 6px; }
.progress-text { margin-top: 8px; color: #64748B; font-size: 12px; }
.card-actions { margin-top: 4px; }
.learning-drawer :deep(.el-drawer__body) { background: rgba(248, 250, 252, 0.72); }
.drawer-empty { color: #64748B; padding: 12px 4px; }
.chapter-grid { display: grid; gap: 12px; }
.chapter-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
}
.chapter-left { flex: 1; min-width: 0; }
.chapter-title { color: #1E293B; font-weight: 600; }
.chapter-content {
  margin-top: 8px;
  color: #64748B;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}
.chapter-right { display: flex; align-items: center; min-width: 130px; justify-content: flex-end; }
.done-tag { display: inline-flex; align-items: center; gap: 4px; }
</style>
