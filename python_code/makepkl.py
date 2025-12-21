import json
import pandas as pd
import pickle
import torch
from sentence_transformers import SentenceTransformer
import re

print("1. 데이터를 읽고 있습니다...")
with open('course_data_final.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
df = pd.DataFrame(data)

# 전처리
df['prefix'] = df['course_code'].apply(lambda x: re.match(r"([A-Za-z]+)", x).group(1) if x else "ETC")

def combine_text(row):
    desc = " ".join(row['description_keywords']) if isinstance(row['description_keywords'], list) else ""
    career = " ".join(row['career_keywords']) if isinstance(row['career_keywords'], list) else ""
    return f"과목명은 {row['course_name']}입니다. 관련 키워드는 {desc}이고, 진로는 {career}와 관련 있습니다."

df['combined_text'] = df.apply(combine_text, axis=1)

print("2. AI 모델 로딩 & 양자화 중...")
model = SentenceTransformer('jhgan/ko-sroberta-multitask', device='cpu')

# 양자화 적용 (속도 향상)
model = torch.quantization.quantize_dynamic(
    model, {torch.nn.Linear}, dtype=torch.qint8
)

print("3. 텍스트를 벡터로 변환 중... (이 과정이 제일 오래 걸립니다. 멈춘 거 아니니 기다리세요!)")
# 여기서 시간이 많이 걸립니다.
embeddings = model.encode(df['combined_text'].tolist(), show_progress_bar=True)

print("4. 결과를 파일로 저장 중...")
# 데이터프레임과 임베딩 결과를 한 파일에 묶어서 저장
with open('course_vectors.pkl', 'wb') as f:
    pickle.dump((df, embeddings), f)

print("✅ 저장 완료! 이제 'course_vectors.pkl' 파일이 생겼습니다.")
~
