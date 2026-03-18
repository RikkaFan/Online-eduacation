import { API_BASE, getAuthHeaders } from './request';
import { getCourses } from './course';

const EXAM_API = `${API_BASE}/api`;

// 获取某课程下的考试列表
export async function getExamsByCourse(courseId) {
  const res = await fetch(`${EXAM_API}/courses/${courseId}/exams`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取考试列表失败: ${res.status} ${t}`);
  }
  return res.json();
}

// 聚合获取所有课程下的考试列表（学生端用于“全部考试”视图）
export async function getAllExamsByAllCourses() {
  const courses = await getCourses();
  const tasks = courses.map(c => getExamsByCourse(c.id).then(list => list.map(e => ({ ...e, course: e.course || c }))));
  const chunks = await Promise.all(tasks);
  return chunks.flat();
}

// 获取单场考试详情（包含课程、时间等；题目通常随实体返回）
export async function getExamDetail(examId) {
  const res = await fetch(`${EXAM_API}/exams/${examId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取考试详情失败: ${res.status} ${t}`);
  }
  return res.json();
}

// 提交试卷：后端期待 List<StudentAnswer>
// 结构：[{ question: { id }, selectedAnswer }]
export async function submitExam(examId, answers) {
  const res = await fetch(`${EXAM_API}/exams/${examId}/submit`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(answers),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`提交试卷失败: ${res.status} ${t}`);
  }
  return res.json();
}

