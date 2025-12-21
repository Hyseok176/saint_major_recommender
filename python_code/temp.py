import pickle

import torch

from sentence_transformers import SentenceTransformer

from sklearn.metrics.pairwise import cosine_similarity

import sys

import pandas as pd



# ==========================================

# 1. 저장된 파일 불러오기 (초고속 로딩)

# ==========================================

print("🚀 [테스트 모드] 저장된 데이터를 불러옵니다...")



try:

    # 1. 데이터 로딩 (pkl 파일에서 읽기)

    # make_vectors.py에서 이미 전처리(prefix 등)가 끝난 df가 저장되어 있습니다.

    with open('course_vectors.pkl', 'rb') as f:

        df, course_embeddings = pickle.load(f)

    print("   ㄴ 데이터 로딩 완료!")



    # 2. 모델 로딩 (질문 해석용, CPU 모드 & 양자화)

    print("   ㄴ AI 모델 로딩 중...")

    model = SentenceTransformer('jhgan/ko-sroberta-multitask', device='cpu')



    # 양자화 적용 (속도 향상)

    model = torch.quantization.quantize_dynamic(

        model, {torch.nn.Linear}, dtype=torch.qint8

    )

    print("✅ 준비 완료! 시스템이 준비되었습니다.")



except FileNotFoundError:

    print("❌ [오류] 'course_vectors.pkl' 파일이 없습니다.")

    print("   먼저 make_vectors.py를 실행해서 파일을 만들어주세요!")

    sys.exit()





# ==========================================

# 2. 추천 엔진 (AI 문맥 검색)

# ==========================================

def recommend_courses(query, major_prefixes=None, top_n=5):



    # (1) 전공 필터링

    if isinstance(major_prefixes, str):

        major_list = [major_prefixes]

    elif isinstance(major_prefixes, (list, tuple)):

        major_list = list(major_prefixes)

    else:

        major_list = None



    if major_list:

        # pickle에 저장된 df에 이미 'prefix' 컬럼이 있습니다.

        mask = df['prefix'].isin(major_list)

        target_indices = df[mask].index

        if len(target_indices) == 0:

            return [], f"해당 전공 코드({', '.join(major_list)})를 가진 과목이 없습니다."



        target_embeddings = course_embeddings[target_indices]

        filtered_df = df.loc[target_indices]

    else:

        target_embeddings = course_embeddings

        filtered_df = df



    # (2) 문장 유사도 계산

    query_embedding = model.encode([query])



    # 코사인 유사도 계산

    scores = cosine_similarity(query_embedding, target_embeddings).flatten()



    # (3) 정렬 및 상위 N개 추출

    sorted_indices = scores.argsort()[:-top_n-1:-1]



    results = []

    for i in sorted_indices:

        score = scores[i]

        if score > 0.2:

            row = filtered_df.iloc[i]

            results.append({

                'name': row['course_name'],

                'code': row['course_code'],

                'score': float(score),

                'keywords': row['description_keywords']

            })



    return results, None





# ==========================================

# 3. 사용자 인터페이스 (원하시는 메뉴 형태)

# ==========================================

def main():

    available_majors = sorted(df['prefix'].unique())



    while True:

        print("\n" + "="*70)

        print("🤖 AI 문맥 기반 과목 추천 시스템 (Fast Mode)")

        print("1. 전공별 맞춤 추천")

        print("2. 문장으로 자유롭게 물어보기 (전공 무관)")

        print("q. 종료")

        print("="*70)



        choice = input("선택 (1/2/q): ").strip().lower()



        if choice in ['q', 'quit', 'exit']:

            print("프로그램을 종료합니다.")

            break



        elif choice == '1':

            print(f"가능 전공: {', '.join(available_majors)}")

            major_input = input("전공 코드 입력 (예: CSE): ").strip().upper()



            # 쉼표로 구분된 입력 처리

            selected_majors = [m.strip() for m in major_input.split(',') if m.strip()]



            # 유효성 검사

            valid_majors = [m for m in selected_majors if m in available_majors]



            if not valid_majors:

                print("⚠️ 유효한 전공 코드가 없습니다.")

                continue



            query = input("관심사나 진로를 문장으로 적어주세요: ").strip()

            if not query: continue



            print(f"\n🔍 '{query}' 내용을 분석 중...\n")



            for major in valid_majors:

                print(f"📘 [{major}] 추천 결과")

                results, msg = recommend_courses(query, major_prefixes=major)

                if results:

                    for idx, item in enumerate(results, 1):

                        kwd = ", ".join(item['keywords'][:3]) if isinstance(item['keywords'], list) else ""

                        print(f"   {idx}. {item['name']} ({item['score']:.2f}) - {kwd}...")

                else:

                    print(f"   ❌ {msg or '추천 데이터 없음'}")

                print("-" * 30)



        elif choice == '2':

            print("💡 예시: '요즘 너무 우울한데 힐링되는 수업 듣고 싶어', '창업해서 돈 많이 벌고 싶어'")

            query = input("질문 입력: ").strip()

            if not query: continue



            print(f"\n🔍 전체 과목에서 의미가 유사한 수업을 찾는 중...")



            results, _ = recommend_courses(query, top_n=5)



            if results:

                print("-" * 60)

                for idx, item in enumerate(results, 1):

                    kwd = ", ".join(item['keywords'][:3]) if isinstance(item['keywords'], list) else ""

                    print(f"{idx}. [{item['code']}] {item['name']}")

                    print(f"   └ 유사도: {item['score']:.2f} | 키워드: {kwd}...")

            else:

                print("❌ 관련성 높은 과목을 찾지 못했습니다.")



        else:

            print("잘못된 입력입니다.")



if __name__ == "__main__":

    main()

