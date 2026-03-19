import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api`;

export async function getChapters(courseId) {
  const res = await fetch(`${API}/courses/${courseId}/chapters`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取课时列表失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function addChapter(courseId, chapterData) {
  const res = await fetch(`${API}/courses/${courseId}/chapters`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(chapterData),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`发布课时失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function deleteChapter(chapterId) {
  const res = await fetch(`${API}/chapters/${chapterId}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`删除课时失败: ${res.status} ${t}`);
  }
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return res.json();
  return res.text();
}

export async function completeChapter(chapterId) {
  const res = await fetch(`${API}/chapters/${chapterId}/complete`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`打卡失败: ${res.status} ${t}`);
  }
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) return res.json();
  return res.text();
}

export async function getCourseProgress(courseId) {
  const res = await fetch(`${API}/courses/${courseId}/progress`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取学习进度失败: ${res.status} ${t}`);
  }
  return res.json();
}
