import json
import os
import pickle
import torch
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import sys
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI
from typing import List, Optional

# ==========================================
# 1. FastAPI 앱 설정 및 데이터 로딩 (전역)
# ==========================================
app = FastAPI()

# CORS 설정 (스프링 부트 연동용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "https://saintplanner.cloud"],
    allow_methods=["*"],
    allow_headers=["*"],
)

openai_api_key = os.getenv("OPENAI_API_KEY")
openai_model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
openai_client = OpenAI(api_key=openai_api_key) if openai_api_key else None

print("🚀 [서버 시작] 데이터와 AI 모델을 불러옵니다...")

try:
    # 1. 데이터 로딩 (pkl 파일)
    with open('course_vectors.pkl', 'rb') as f:
        df, course_embeddings = pickle.load(f)
    print("   ㄴ 데이터 로딩 완료!")

    # 2. 모델 로딩 (CPU 모드 & 양자화)
    model = SentenceTransformer('jhgan/ko-sroberta-multitask', device='cpu')
    model = torch.quantization.quantize_dynamic(
        model, {torch.nn.Linear}, dtype=torch.qint8
    )
    print("✅ 시스템 준비 완료! 요청을 기다립니다.")

except FileNotFoundError:
    print("❌ [오류] 'course_vectors.pkl' 파일이 없습니다.")
    sys.exit(1)


# ==========================================
# 2. 데이터 통신 규격 정의 (DTO)
# ==========================================

# [요청] 스프링 부트에서 보낼 데이터
class UserRequest(BaseModel):
    prompt: str          # 예: "창업 관련 수업 추천해줘"
    target: str          # 예: "CSE" (전공) 또는 "GE" (교양 - 실제 데이터의 prefix에 맞춰야 함)
    threshold: float = 0.25 # (선택) 이 점수 미만은 버림 (기본값 0.25)

# [응답] 리스트 안에 들어갈 개별 아이템
class CourseItem(BaseModel):
    code: str            # 과목 코드 (예: "CSE405")
    score: float         # 유사도 점수 (예: 0.8211)
    reason: Optional[str] = None  # LLM이 생성한 추천 이유

# [응답] 최종 반환 데이터
class AiResponse(BaseModel):
    results: List[CourseItem]


def _build_course_snapshot(row) -> dict:
    return {
        "course_code": row.get("course_code", ""),
        "course_name": row.get("course_name", ""),
        "description_keywords": row.get("description_keywords", []),
        "career_keywords": row.get("career_keywords", []),
    }


def _generate_reasons_with_llm(prompt: str, target: str, ranked_courses: List[dict]) -> dict:
    if not openai_client or not ranked_courses:
        return {}

    courses_payload = []
    for course in ranked_courses:
        keywords = course["description_keywords"]
        careers = course["career_keywords"]
        courses_payload.append({
            "course_code": course["course_code"],
            "course_name": course["course_name"],
            "score": course["score"],
            "description_keywords": keywords if isinstance(keywords, list) else [],
            "career_keywords": careers if isinstance(careers, list) else [],
        })

    messages = [
        {
            "role": "system",
            "content": (
                "당신은 과목 추천 설명 도우미입니다. "
                "반드시 JSON 객체만 반환하세요. "
                "형식: {\"reasons\": [{\"course_code\": \"...\", \"reason\": \"...\"}]}"
            ),
        },
        {
            "role": "user",
            "content": (
                f"사용자 질문: {prompt}\n"
                f"전공 prefix: {target}\n"
                f"후보 과목(JSON): {json.dumps(courses_payload, ensure_ascii=False)}\n\n"
                "요구사항:\n"
                "1) 각 과목마다 추천 이유를 1~2문장 한국어로 작성\n"
                "2) 질문/키워드/진로 연관성 중심으로 설명\n"
                "3) 제공된 정보 밖의 추측 금지\n"
                "4) JSON 외 텍스트 금지"
            ),
        },
    ]

    response = openai_client.chat.completions.create(
        model=openai_model,
        messages=messages,
        temperature=0.2,
    )

    content = response.choices[0].message.content if response.choices else ""
    if not content:
        return {}

    try:
        parsed = json.loads(content)
    except json.JSONDecodeError:
        return {}

    reason_map = {}
    for item in parsed.get("reasons", []):
        code = item.get("course_code")
        reason = item.get("reason")
        if code and reason:
            reason_map[code] = reason
    return reason_map


# ==========================================
# 3. 핵심 로직 및 API 엔드포인트
# ==========================================

@app.post("/recommend", response_model=AiResponse)
async def recommend_courses(req: UserRequest):
    """
    사용자의 질문(prompt)과 타겟 전공(target)을 받아
    유사도가 높은 과목 코드와 점수를 리스트로 반환합니다.
    """
    
    # (1) 전공/교양 필터링
    # 요청받은 target(예: "CSE")과 데이터의 'prefix' 컬럼이 일치하는지 확인
    mask = df['prefix'] == req.target
    target_indices = df[mask].index

    # 해당 전공 코드가 하나도 없으면 빈 리스트 반환
    if len(target_indices) == 0:
        return {"results": []}

    # 필터링된 데이터만 추출
    target_embeddings = course_embeddings[target_indices]
    filtered_df = df.loc[target_indices]

    # (2) 문장 유사도 계산
    # 사용자의 질문을 벡터로 변환
    query_embedding = model.encode([req.prompt])
    
    # 전체 후보군과 코사인 유사도 계산
    scores = cosine_similarity(query_embedding, target_embeddings).flatten()

    # (3) 정렬 및 결과 포장
    # 점수가 높은 순서대로 인덱스 정렬
    sorted_indices = scores.argsort()[::-1]

    top_k = 8
    items = []
    ranked_courses = []
    for i in sorted_indices:
        score = float(scores[i])
        
        # 설정한 점수(threshold) 이상인 경우에만 결과에 포함
        if score >= req.threshold:
            row = filtered_df.iloc[i]
            snapshot = _build_course_snapshot(row)
            snapshot["score"] = round(score, 4)
            ranked_courses.append(snapshot)
            
            # 응답 객체 생성
            item = CourseItem(
                code=row['course_code'],
                score=round(score, 4)  # 소수점 4자리까지 반올림
            )
            items.append(item)

            if len(items) >= top_k:
                break

    reason_map = _generate_reasons_with_llm(req.prompt, req.target, ranked_courses)
    for item in items:
        item.reason = reason_map.get(item.code)

    # 최종 결과 반환
    return {"results": items}
