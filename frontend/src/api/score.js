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

export async function getExamReview(examId) {
  const res = await fetch(`${API}/results/review/${examId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取试卷回放失败: ${res.status} ${t}`);
  }
  return res.json();
}

export function exportScoreToExcel(examId) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', `${API}/exams/${examId}/export`, true);
    const headers = getAuthHeaders();
    Object.entries(headers).forEach(([key, value]) => {
      if (key.toLowerCase() !== 'content-type') {
        xhr.setRequestHeader(key, value);
      }
    });
    xhr.responseType = 'blob';

    xhr.onload = () => {
      if (xhr.status < 200 || xhr.status >= 300) {
        reject(new Error(`导出成绩失败: ${xhr.status}`));
        return;
      }
      const disposition = xhr.getResponseHeader('content-disposition') || '';
      const match = disposition.match(/filename\*=utf-8''([^;]+)/i);
      const fileName = match && match[1]
        ? decodeURIComponent(match[1])
        : `exam_scores_${examId}.xlsx`;
      resolve({
        blob: xhr.response,
        fileName,
      });
    };

    xhr.onerror = () => reject(new Error('导出成绩失败: 网络异常'));
    xhr.send();
  });
}

export async function getPendingGrading() {
  const res = await fetch(`${API}/results/pending-grading`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取待批改列表失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function getGradedResults() {
  const res = await fetch(`${API}/results/graded`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取已批阅列表失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function autoGradeWithAI(studentAnswerId) {
  const res = await fetch(`${API}/results/grade/ai`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ studentAnswerId }),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`AI 批改失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function manualGrade(studentAnswerId, score) {
  const res = await fetch(`${API}/results/grade/manual`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ studentAnswerId, score }),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`手动给分失败: ${res.status} ${t}`);
  }
  return res.json();
}
