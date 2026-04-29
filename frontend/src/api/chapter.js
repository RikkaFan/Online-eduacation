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

export async function getCompletedChapterIds(courseId) {
  const res = await fetch(`${API}/courses/${courseId}/completed-chapters`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取已完成课时失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function uploadChapterMaterial(chapterId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const headers = getAuthHeaders();
  delete headers['Content-Type'];
  const res = await fetch(`${API}/chapters/${chapterId}/material`, {
    method: 'POST',
    headers,
    body: formData,
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`上传资料失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function downloadChapterMaterial(chapterId) {
  const res = await fetch(`${API}/chapters/${chapterId}/material`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`下载资料失败: ${res.status} ${t}`);
  }
  const disposition = res.headers.get('content-disposition') || '';
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1] || '';
  const fallback = disposition.match(/filename="?([^"]+)"?/i)?.[1] || 'course-material.bin';
  const fileName = encoded ? decodeURIComponent(encoded) : fallback;
  const blob = await res.blob();
  return { blob, fileName };
}
