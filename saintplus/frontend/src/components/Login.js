import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import './Login.css';

// Figma에서 가져온 이미지 에셋 (public 폴더)
const logoCircle = "/da57dfd4a0030394be8368137a63e83e01132aaf.png";
const logoText = "/5b741b783786f30c11cf57dc24cb8de80d28df92.png";

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!username.trim() || !password.trim()) {
      setError('아이디와 비밀번호를 모두 입력해주세요.');
      return;
    }

    setLoading(true);

    try {
      // 백엔드 로그인 API 호출
      const response = await login(username, password);
      
      if (response.success && response.token) {
        // JWT 토큰 저장
        localStorage.setItem('token', response.token);
        localStorage.setItem('user', JSON.stringify(response.user));
        
        // 최초 로그인 설정 페이지로 이동
        navigate('/setup');
      } else {
        setError('로그인에 실패했습니다.');
      }
    } catch (err) {
      console.error('로그인 실패:', err);
      if (err.response?.status === 401) {
        setError('아이디 또는 비밀번호가 잘못되었습니다.');
      } else {
        setError('로그인에 실패했습니다. 다시 시도해주세요.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-background">
        {/* Figma 디자인: 로고 */}
        <div className="logo-section">
          <div className="logo-wrapper">
            <img src={logoCircle} alt="Saint+ Logo" className="logo-circle-img" />
          </div>
          <div className="logo-text-wrapper">
            <img src={logoText} alt="Saint+" className="logo-text-img" />
          </div>
        </div>

        {/* 로그인 폼 (카카오 로그인 대신 일반 로그인) */}
        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="아이디"
              disabled={loading}
              autoComplete="username"
            />
          </div>

          <div className="input-group">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
              disabled={loading}
              autoComplete="current-password"
            />
          </div>

          {error && <div className="error-message">{error}</div>}

          <button type="submit" className="login-button" disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className="signup-section">
          <p>계정이 없으신가요? <a href="/register">회원가입</a></p>
        </div>
      </div>
    </div>
  );
}

export default Login;
