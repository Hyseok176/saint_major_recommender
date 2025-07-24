# 수강 기록 분석 프로젝트

## 1. 프로젝트 실행 방법

### 1.1. 환경 설정

*   Java Development Kit (JDK) 11 이상 설치
*   `JAVA_HOME` 환경 변수 설정

### 1.2. 애플리케이션 실행

1.  프로젝트 루트 디렉토리(`C:\banghak`)로 이동합니다.
2.  아래 명령어를 실행하여 Spring Boot 애플리케이션을 시작합니다.

    ```bash
    ./gradlew bootRun
    ```

3.  웹 브라우저에서 `http://localhost:8080`으로 접속합니다.

## 2. 프로젝트 수정 방법

### 2.1. 주요 파일 위치

*   **백엔드 (Java):** `C:\banghak\course_analyzer\src\main\java\com\example\course_analyzer`
    *   `CourseController.java`: HTTP 요청 처리 및 페이지 라우팅
    *   `CourseService.java`: 핵심 비즈니스 로직 (파일 분석, 데이터 처리)
    *   `User.java`, `SemesterCourse.java`: 데이터베이스 엔티티
*   **프론트엔드 (HTML/Thymeleaf):** `C:\banghak\course_analyzer\src\main\resources\templates`
    *   `index.html`: 로그인 및 회원가입 페이지
    *   `upload-file.html`: 파일 업로드 페이지
    *   `results.html`: 분석 결과 표시 페이지
*   **설정 파일:** `C:\banghak\course_analyzer\src\main\resources\application.properties`
    *   데이터베이스 연결 정보, 서버 포트 등 설정

### 2.2. 코드 수정 및 빌드

1.  원하는 IDE(IntelliJ, Eclipse 등)를 사용하여 코드를 수정합니다.
2.  수정 후, 위 "1.2. 애플리케이션 실행" 항목의 명령어를 다시 실행하여 변경 사항을 적용합니다.

## 3. 데이터베이스 확인

*   H2 데이터베이스는 파일 기반으로 설정되어 있으며, `C:/Users/사용자명/course_analyzer/temp/` 경로에 `course_analyzer_db.mv.db` 파일로 저장됩니다.
*   H2 콘솔(`http://localhost:8080/h2-console`)에 접속하여 데이터베이스 내용을 직접 확인할 수 있습니다. (JDBC URL, 사용자명, 비밀번호는 `application.properties` 파일 참조)

```