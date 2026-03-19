import { API_BASE, getAuthHeaders } from './request';

const API = `${API_BASE}/api`;

export async function getLogs() {
  const res = await fetch(`${API}/logs`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`获取日志列表失败: ${res.status} ${t}`);
  }
  return res.json();
}
