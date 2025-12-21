import api from './api';

// 과목 추천 API
export const getRecommendations = async (semester) => {
  const params = semester ? { semester } : {};
  const response = await api.get('/api/recommendations', { params });
  return response.data;
};

// AI 기반 과목 추천 API
export const getAiRecommendations = async (prompt, major) => {
  const response = await api.get('/api/ai-recommend', {
    params: { prompt, major },
  });
  return response.data;
};

// 수강 결과 데이터 조회
export const getResults = async () => {
  const response = await api.get('/results');
  return response.data;
};

// 전체 과목 목록 조회
export const getAllCourses = async (filters) => {
  const response = await api.get('/all-courses', { params: filters });
  return response.data;
};

// 과목 상세 정보 조회
export const getCourseDetail = async (courseId) => {
  const response = await api.get(`/api/courses/${courseId}`);
  return response.data;
};

// 장바구니에 과목 추가
export const addToCart = async (courseId) => {
  const response = await api.post(`/api/courses/${courseId}/cart`);
  return response.data;
};

// 장바구니에서 과목 삭제
export const removeFromCart = async (courseId) => {
  const response = await api.delete(`/api/courses/${courseId}/cart`);
  return response.data;
};

// 과목 검색
export const searchCourses = async (query) => {
  const response = await api.get('/api/courses/search', {
    params: { q: query },
  });
  return response.data;
};
