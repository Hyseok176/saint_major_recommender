import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import InitialSetup from './components/InitialSetup';
import Recommendation from './components/Recommendation';
import TranscriptUpload from './components/TranscriptUpload';
import Recommendations from './components/Recommendations';
import AiRecommendations from './components/AiRecommendations';
import CourseSearch from './components/CourseSearch';
import './App.css';

// 로그인 여부 확인 함수
function isAuthenticated() {
  return localStorage.getItem('user') !== null;
}

// Protected Route 컴포넌트
function ProtectedRoute({ children }) {
  return isAuthenticated() ? children : <Navigate to="/login" replace />;
}

function App() {
  const handleLogout = () => {
    localStorage.removeItem('user');
    window.location.href = '/login';
  };

  return (
    <Router>
      <div className="App">
        <Routes>
          {/* 로그인, 회원가입 페이지 */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* 최초 로그인 설정 페이지 */}
          <Route 
            path="/setup" 
            element={
              <ProtectedRoute>
                <InitialSetup />
              </ProtectedRoute>
            } 
          />

          {/* 추천 페이지 */}
          <Route 
            path="/recommend" 
            element={
              <ProtectedRoute>
                <Recommendation />
              </ProtectedRoute>
            } 
          />

          {/* 보호된 라우트들 */}
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <div>
                  <header className="app-header">
                    <div className="header-content">
                      <h1>Saint+ 전공 추천 시스템</h1>
                      <nav className="main-nav">
                        <Link to="/">홈</Link>
                        <Link to="/upload">성적표 업로드</Link>
                        <Link to="/recommendations">통계 추천</Link>
                        <Link to="/ai-recommendations">AI 추천</Link>
                        <Link to="/search">과목 검색</Link>
                        <button onClick={handleLogout} className="logout-btn">
                          로그아웃
                        </button>
                      </nav>
                    </div>
                  </header>

                  <main className="app-main">
                    <Routes>
                      <Route path="/" element={<Home />} />
                      <Route path="/upload" element={<TranscriptUpload />} />
                      <Route path="/recommendations" element={<Recommendations />} />
                      <Route path="/ai-recommendations" element={<AiRecommendations />} />
                      <Route path="/search" element={<CourseSearch />} />
                    </Routes>
                  </main>

                  <footer className="app-footer">
                    <p>&copy; 2025 Saint+ Project. All rights reserved.</p>
                  </footer>
                </div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </div>
    </Router>
  );
}

function Home() {
  return (
    <div className="home">
      <div className="hero-section">
        <h2>서강대 학부생을 위한 전공 추천 시스템</h2>
        <p>AI와 통계 기반으로 당신에게 맞는 과목을 추천해드립니다.</p>
      </div>

      <div className="features">
        <div className="feature-card">
          <h3>📄 성적표 업로드</h3>
          <p>성적표를 업로드하면 자동으로 수강 이력을 분석합니다.</p>
          <Link to="/upload" className="feature-link">시작하기 →</Link>
        </div>

        <div className="feature-card">
          <h3>📊 통계 기반 추천</h3>
          <p>선배들의 수강 패턴을 분석하여 과목을 추천합니다.</p>
          <Link to="/recommendations" className="feature-link">보러가기 →</Link>
        </div>

        <div className="feature-card">
          <h3>🤖 AI 기반 추천</h3>
          <p>당신의 관심사와 목표에 맞는 과목을 AI가 추천합니다.</p>
          <Link to="/ai-recommendations" className="feature-link">시작하기 →</Link>
        </div>

        <div className="feature-card">
          <h3>🔍 과목 검색</h3>
          <p>과목명, 교수명, 과목코드로 원하는 과목을 찾아보세요.</p>
          <Link to="/search" className="feature-link">검색하기 →</Link>
        </div>
      </div>
    </div>
  );
}

export default App;
