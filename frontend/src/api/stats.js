import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api/stats`;

export async function getAdminStats() {
  const res = await fetch(`${API}/admin`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取管理端统计失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function getStudentStats(studentId) {
  const res = await fetch(`${API}/student/${studentId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取学生统计失败: ${res.status} ${t}`);
  }
  return res.json();
}
