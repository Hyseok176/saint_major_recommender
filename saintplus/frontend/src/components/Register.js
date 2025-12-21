import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { register } from '../api/authApi';
import './Register.css';

// Figma에서 가져온 이미지 에셋 (public 폴더)
const logoCircle = "/da57dfd4a0030394be8368137a63e83e01132aaf.png";
const logoText = "/5b741b783786f30c11cf57dc24cb8de80d28df92.png";

function Register() {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    nickname: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // 유효성 검사
    if (!formData.username.trim() || !formData.password.trim()) {
      setError('아이디와 비밀번호는 필수입니다.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    if (formData.password.length < 6) {
      setError('비밀번호는 최소 6자 이상이어야 합니다.');
      return;
    }

    setLoading(true);

    try {
      // 백엔드 회원가입 API 호출
      const response = await register(
        formData.username,
        formData.password,
        formData.nickname || null,
        formData.email || null
      );
      
      alert(response.message || '회원가입이 완료되었습니다! 로그인해주세요.');
      navigate('/login');
    } catch (err) {
      console.error('회원가입 실패:', err);
      if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err.response?.status === 400) {
        setError('이미 존재하는 아이디이거나 입력 정보가 유효하지 않습니다.');
      } else {
        setError('회원가입에 실패했습니다. 다시 시도해주세요.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-container">
      <div className="register-background">
        {/* Figma 디자인: 로고 */}
        <div className="logo-section">
          <div className="logo-wrapper">
            <img src={logoCircle} alt="Saint+ Logo" className="logo-circle-img" />
          </div>
          <div className="logo-text-wrapper">
            <img src={logoText} alt="Saint+" className="logo-text-img" />
          </div>
        </div>

          <form onSubmit={handleSubmit} className="register-form">
            <div className="input-group">
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="아이디 *"
                disabled={loading}
                autoComplete="username"
              />
            </div>

            <div className="input-group">
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="비밀번호 *"
                disabled={loading}
                autoComplete="new-password"
              />
            </div>

            <div className="input-group">
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="비밀번호 확인 *"
                disabled={loading}
                autoComplete="new-password"
              />
            </div>

            <div className="input-group">
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="이메일"
                disabled={loading}
                autoComplete="email"
              />
            </div>

            <div className="input-group">
              <input
                type="text"
                name="nickname"
                value={formData.nickname}
                onChange={handleChange}
                placeholder="닉네임"
                disabled={loading}
              />
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" className="register-button" disabled={loading}>
              {loading ? '가입 중...' : '회원가입'}
            </button>
          </form>

        <div className="login-link-section">
          <p>이미 계정이 있으신가요? <a href="/login">로그인</a></p>
        </div>
      </div>
    </div>
  );
}

export default Register;
