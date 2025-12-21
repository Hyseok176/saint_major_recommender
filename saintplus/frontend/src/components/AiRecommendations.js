import React, { useState } from 'react';
import { getAiRecommendations } from '../api/courseApi';
import './AiRecommendations.css';

function AiRecommendations() {
  const [prompt, setPrompt] = useState('');
  const [major, setMajor] = useState('');
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const majors = [
    '컴퓨터공학과',
    '경영학과',
    '경제학과',
    '수학과',
    '물리학과',
    '화학과',
    '생명과학과',
    '영어영문학과',
    '철학과',
    '사학과',
    '기계공학과',
    '전자공학과',
    // 추가 전공들...
  ];

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!prompt.trim() || !major) {
      setError('프롬프트와 전공을 모두 입력해주세요.');
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const data = await getAiRecommendations(prompt, major);
      setRecommendations(data);
    } catch (err) {
      console.error('AI 추천 실패:', err);
      setError('AI 추천을 가져오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-recommendations">
      <h2>AI 기반 과목 추천</h2>
      
      <form onSubmit={handleSubmit} className="ai-form">
        <div className="form-group">
          <label>전공 선택</label>
          <select
            value={major}
            onChange={(e) => setMajor(e.target.value)}
            required
          >
            <option value="">전공을 선택하세요</option>
            {majors.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>관심 분야나 목표를 입력하세요</label>
          <textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="예: 인공지능과 머신러닝에 관심이 있어요. 딥러닝 관련 과목을 추천해주세요."
            rows="4"
            required
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? 'AI가 분석 중입니다...' : '추천 받기'}
        </button>
      </form>

      {error && <div className="error-message">{error}</div>}

      {recommendations.length > 0 && (
        <div className="results">
          <h3>추천 과목</h3>
          <div className="recommendations-list">
            {recommendations.map((course, index) => (
              <div key={index} className="course-card">
                <h4>{course.courseName}</h4>
                <p className="course-code">{course.courseCode}</p>
                <p className="reason">{course.reason}</p>
                <div className="course-details">
                  <span>학점: {course.credit}</span>
                  <span>교수: {course.professor}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default AiRecommendations;
