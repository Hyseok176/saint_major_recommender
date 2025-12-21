import api from './api';

// 회원가입
export const register = async (username, password, nickname, email) => {
  const response = await api.post('/api/auth/register', {
    username,
    password,
    nickname: nickname || null,
    email: email || null,
  });
  return response.data;
};

// 로그인
export const login = async (username, password) => {
  const response = await api.post('/api/auth/login', {
    username,
    password,
  });
  return response.data;
};

// 전공 정보 업데이트
export const updateMajors = async (major1, major2, major3) => {
  const response = await api.post('/api/auth/update-majors', {
    major1,
    major2,
    major3,
  });
  return response.data;
};
