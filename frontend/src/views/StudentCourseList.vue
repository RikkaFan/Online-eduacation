<template>
  <div class="student-course-list">
    <header class="dashboard-header">
      <h1>我的课程</h1>
      <div class="course-filter-tabs glass-chip">
        <el-button :type="onlyEnrolled ? 'primary' : 'default'" round @click="onlyEnrolled = true">仅看已选</el-button>
        <el-button :type="!onlyEnrolled ? 'primary' : 'default'" round @click="onlyEnrolled = false">全部课程</el-button>
      </div>
    </header>
    <el-row :gutter="24">
      <el-col :span="6" v-for="c in visibleCourses" :key="c.id" style="margin-bottom: 24px;">
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
              <el-button v-if="isEnrolled(c.id)" type="primary" round @click="openLearningDrawer(c)">
                {{ Number(c.progressData?.percentage || 0) > 0 ? '继续学习' : '开始学习' }}
              </el-button>
              <el-button v-else type="success" plain round :loading="courseOperatingId === c.id" @click="handleEnroll(c)">
                立即选课
              </el-button>
              <el-button
                v-if="isEnrolled(c.id)"
                type="danger"
                plain
                round
                :loading="courseOperatingId === c.id"
                @click="handleUnenroll(c)"
              >
                退课
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
      <div class="drawer-tools">
        <el-button type="warning" plain @click="openEvaluationDialog">
          <el-icon><StarFilled /></el-icon>
          评价本课程
        </el-button>
      </div>
      <div v-if="chapterLoading" class="drawer-empty">正在加载课时内容...</div>
      <div v-else-if="activeChapters.length === 0" class="drawer-empty">当前课程暂未发布课时</div>
      <div v-else class="chapter-grid">
        <div v-for="(chapter, index) in activeChapters" :key="chapter.id" class="chapter-card glass-card">
          <div class="chapter-left">
            <div class="chapter-title">第 {{ index + 1 }} 节：{{ chapter.title || '未命名课时' }}</div>
            <div class="chapter-content">{{ chapter.content || '暂无课时内容' }}</div>
            <div v-if="chapter.materialName" class="material-row">
              <el-button link type="primary" :icon="Download" @click="handleDownloadMaterial(chapter)">
                下载资料：{{ chapter.materialName }}
              </el-button>
            </div>
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

    <el-dialog v-model="evaluationDialogVisible" title="课程评价" width="520px">
      <el-form label-width="80px">
        <el-form-item label="星级评分">
          <el-rate v-model="evaluationForm.rating" show-text />
        </el-form-item>
        <el-form-item label="课程评语">
          <el-input
            v-model="evaluationForm.comment"
            type="textarea"
            :rows="4"
            placeholder="说说你对这门课的看法吧..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evaluationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingEvaluation" @click="submitCourseEvaluation">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Download, Select, StarFilled } from '@element-plus/icons-vue';
import { enrollCourse, getCourses, getMyEnrolledCourses, unenrollCourse } from '@/api/course';
import { completeChapter, downloadChapterMaterial, getChapters, getCourseProgress } from '@/api/chapter';
import { submitEvaluation } from '@/api/evaluation';

const courses = ref([]);
const enrolledCourseIds = ref(new Set());
const onlyEnrolled = ref(true);
const courseOperatingId = ref(null);
const learningDrawerVisible = ref(false);
const chapterLoading = ref(false);
const activeCourse = ref(null);
const activeChapters = ref([]);
const completingChapterId = ref(null);
const completedChapterIds = ref(new Set());
const evaluationDialogVisible = ref(false);
const submittingEvaluation = ref(false);
const evaluationForm = ref({
  rating: 5,
  comment: '',
});
const visibleCourses = computed(() => {
  if (!onlyEnrolled.value) return courses.value;
  return courses.value.filter(item => enrolledCourseIds.value.has(item.id));
});

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
    const [courseData, enrolledData] = await Promise.all([
      getCourses(),
      getMyEnrolledCourses().catch(() => []),
    ]);
    const courseList = Array.isArray(courseData) ? courseData : [];
    const enrolled = Array.isArray(enrolledData) ? enrolledData : [];
    enrolledCourseIds.value = new Set(enrolled.map(item => item.id));
    courses.value = await hydrateProgress(courseList);
  } catch (e) {
    courses.value = [];
    ElMessage.error(e.message || '加载课程失败');
  }
});

function isEnrolled(courseId) {
  return enrolledCourseIds.value.has(courseId);
}

async function handleEnroll(course) {
  if (!course?.id) return;
  courseOperatingId.value = course.id;
  try {
    await enrollCourse(course.id);
    enrolledCourseIds.value.add(course.id);
    enrolledCourseIds.value = new Set(enrolledCourseIds.value);
    ElMessage.success('选课成功');
  } catch (e) {
    ElMessage.error(e.message || '选课失败');
  } finally {
    courseOperatingId.value = null;
  }
}

async function handleUnenroll(course) {
  if (!course?.id) return;
  courseOperatingId.value = course.id;
  try {
    await unenrollCourse(course.id);
    enrolledCourseIds.value.delete(course.id);
    enrolledCourseIds.value = new Set(enrolledCourseIds.value);
    if (activeCourse.value?.id === course.id) {
      learningDrawerVisible.value = false;
    }
    ElMessage.success('退课成功');
  } catch (e) {
    ElMessage.error(e.message || '退课失败');
  } finally {
    courseOperatingId.value = null;
  }
}

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
  if (!isEnrolled(course.id)) {
    ElMessage.warning('请先选课后再开始学习');
    return;
  }
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

function openEvaluationDialog() {
  if (!activeCourse.value?.id) {
    ElMessage.warning('请先选择课程');
    return;
  }
  evaluationForm.value = {
    rating: 5,
    comment: '',
  };
  evaluationDialogVisible.value = true;
}

async function submitCourseEvaluation() {
  if (!activeCourse.value?.id) return;
  if (!evaluationForm.value.rating) {
    ElMessage.warning('请先完成星级评分');
    return;
  }
  submittingEvaluation.value = true;
  try {
    await submitEvaluation(activeCourse.value.id, {
      rating: evaluationForm.value.rating,
      comment: evaluationForm.value.comment?.trim() || '',
    });
    ElMessage.success('感谢您的评价');
    evaluationDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(e.message || '提交评价失败');
  } finally {
    submittingEvaluation.value = false;
  }
}

async function handleDownloadMaterial(chapter) {
  if (!chapter?.id) return;
  try {
    const { blob, fileName } = await downloadChapterMaterial(chapter.id);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || 'course-material.bin';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } catch (e) {
    ElMessage.error(e.message || '下载资料失败');
  }
}
</script>

<style>
.student-course-list {
  padding: 0 4px;
}
.dashboard-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(226, 232, 240, 0.8); padding-bottom: 16px; margin-bottom: 24px; }
.dashboard-header h1 { margin: 0; font-size: 24px; font-weight: 400; color: #1E293B; }
.course-filter-tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
}
.glass-chip {
  border: 1px solid rgba(212, 224, 244, 0.92);
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px) saturate(170%);
}
.course-filter-tabs .el-button {
  min-width: 96px;
  border: none;
}
.lms-course-card { border-radius: 16px; border: 1px solid rgba(226, 232, 240, 0.8); transition: box-shadow 0.2s; cursor: pointer; background: white; height: 100%; }
.lms-course-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.card-content { padding: 16px; display: grid; gap: 10px; }
.course-code { margin: 0 0 4px 0; font-size: 12px; color: #64748B; font-weight: normal; }
.course-name { margin: 0 0 8px 0; font-size: 16px; font-weight: 700; }
.course-term { margin: 0; font-size: 12px; color: #64748B; }
.progress-wrap { margin-top: 6px; }
.progress-text { margin-top: 8px; color: #64748B; font-size: 12px; }
.card-actions { margin-top: 4px; display: flex; flex-wrap: wrap; gap: 8px; }
.learning-drawer :deep(.el-drawer__body) { background: rgba(248, 250, 252, 0.72); }
.drawer-tools {
  margin-bottom: 12px;
}
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
.material-row { margin-top: 8px; }
</style>
