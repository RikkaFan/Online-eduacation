import { getAuthHeaders, API_BASE } from './request';
const API_URL = `${API_BASE}/api/courses`;

export async function getCourses() {
  const response = await fetch(API_URL, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取课程列表失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function getMyEnrolledCourses() {
  const response = await fetch(`${API_URL}/enrolled/me`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取已选课程失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function enrollCourse(courseId) {
  const response = await fetch(`${API_URL}/${courseId}/enroll`, {
    method: 'POST',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`选课失败: ${response.status} ${errText}`);
  }
  return response.json();
}

export async function unenrollCourse(courseId) {
  const response = await fetch(`${API_URL}/${courseId}/enroll`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`退课失败: ${response.status} ${errText}`);
  }
  return response.json();
}

export async function createCourse(courseData) {
  const response = await fetch(API_URL, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(courseData),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`创建课程失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function deleteCourse(id) {
  const response = await fetch(`${API_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`删除课程失败: ${response.status} ${errText}`);
  }
  
  // 有些 delete 接口可能不返回 body，只返回 200/204
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.indexOf("application/json") !== -1) {
    return response.json();
  } else {
    return response.text();
  }
}
