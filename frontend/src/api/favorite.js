import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api/favorites`;

export async function toggleFavorite(questionId) {
  const res = await fetch(`${API}/${questionId}`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`切换收藏失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function checkFavorite(questionId) {
  const res = await fetch(`${API}/check/${questionId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`检查收藏状态失败: ${res.status} ${t}`);
  }
  return res.json();
}

export async function getFavorites() {
  const res = await fetch(API, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取收藏列表失败: ${res.status} ${t}`);
  }
  return res.json();
}
