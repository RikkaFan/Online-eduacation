import { getAuthHeaders } from './request';
const COURSE_API_URL = 'http://localhost:8081/api/courses';
const EXAM_API_URL = 'http://localhost:8081/api/exams';

export async function getExamsByCourse(courseId) {
  const response = await fetch(`${COURSE_API_URL}/${courseId}/exams`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取考试列表失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function getExamById(id) {
  const response = await fetch(`${EXAM_API_URL}/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`获取考试详情失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function createExam(courseId, examData, numberOfQuestions) {
  const url = `${COURSE_API_URL}/${courseId}/exams?numberOfQuestions=${encodeURIComponent(numberOfQuestions)}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(examData),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`创建考试失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function updateExam(id, examData) {
  const response = await fetch(`${EXAM_API_URL}/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(examData),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`更新考试失败: ${response.status} ${errText}`);
  }

  return response.json();
}

export async function deleteExam(id) {
  const response = await fetch(`${EXAM_API_URL}/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`删除考试失败: ${response.status} ${errText}`);
  }

  const contentType = response.headers.get("content-type");
  if (contentType && contentType.indexOf("application/json") !== -1) {
    return response.json();
  } else {
    return response.text();
  }
}
