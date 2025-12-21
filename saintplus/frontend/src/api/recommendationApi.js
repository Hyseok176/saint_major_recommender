import api from './api';

/**
 * AI 기반 추천 가져오기
 */
export const getAiRecommendations = async (prompt, major) => {
  const response = await api.get('/api/recommendations/ai', {
    params: { prompt, major }
  });
  return response.data;
};

/**
 * 통계 기반 추천 가져오기
 */
export const getStatisticsRecommendations = async () => {
  const response = await api.get('/api/recommendations/statistics');
  return response.data;
};
