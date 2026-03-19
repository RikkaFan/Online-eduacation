import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api`;

export async function submitEvaluation(courseId, data) {
  const response = await fetch(`${API}/courses/${courseId}/evaluations`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`提交评价失败: ${response.status} ${errText}`);
  }
  return response.json();
}

export async function getEvaluations(courseId) {
  const response = await fetch(`${API}/courses/${courseId}/evaluations`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取评价列表失败: ${response.status} ${errText}`);
  }
  return response.json();
}

export async function getEvaluationStats(courseId) {
  const response = await fetch(`${API}/courses/${courseId}/evaluations/stats`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取评价统计失败: ${response.status} ${errText}`);
  }
  return response.json();
}
