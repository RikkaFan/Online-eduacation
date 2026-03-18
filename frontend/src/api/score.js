import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api`;

function getCurrentUserId() {
  try {
    const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
    if (!userStr) return null;
    const user = JSON.parse(userStr);
    return user?.id ?? null;
  } catch {
    return null;
  }
}

// 教师/管理员：根据考试ID获取成绩列表
export async function getScoresByExam(examId) {
  const res = await fetch(`${API}/exams/${examId}/results`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取考试成绩失败: ${res.status} ${t}`);
  }
  return res.json();
}

// 学生端：获取当前登录学生的全部历史成绩
export async function getMyScores() {
  const uid = getCurrentUserId();
  if (!uid) throw new Error('未获取到登录用户ID，无法查询成绩');
  const res = await fetch(`${API}/students/${uid}/results`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取我的成绩失败: ${res.status} ${t}`);
  }
  return res.json();
}

