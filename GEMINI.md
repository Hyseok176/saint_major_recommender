* The user has installed IntelliJ IDEA and will reboot. I need to guide them through license activation (step 3) when they return.
* 현재 진행 중인 프로젝트는 학생들이 대학에서 자신이 수강했던 과목들의 기록이 담긴 텍스트파일을 올리면 정리해서 보여주고 있습니다.
* 모든 개발 작업은 'taeyoon' 브랜치에서 진행됩니다.
* The user will manually install Maven and Gradle using Chocolatey with administrator privileges.
* 사용자가 JAVA\_HOME 환경 변수 설정을 완료했으며, 시스템을 재시작한 후에는 'gradlew bootRun' 명령을 다시 실행하여 Spring Boot 애플리케이션을 시작해야 합니다.
* 앞으로 대화를 종료하기전에 GEMINI.md에 대화내역을 기록해두고, 내가 대화를 시작하기 전에 GEMINI.md파일을 읽어오라고하면 그걸 읽고 적힌 내용을 토대로 대화를 이어나갈 것입니다.
* 현재 웹사이트는 다음 3단계로 구성됩니다:
    1.  **로그인 페이지**: 사용자가 로그인합니다.
    2.  **파일 입력 페이지**: 로그인 후 텍스트 파일을 입력합니다.
    3.  **결과 페이지**: 파일 입력 후 결과를 확인합니다.

* **최근 작업 요약:**
    *   H2 데이터베이스를 사용하여 사용자 정보 및 수강 기록을 영구 저장하도록 구현 중.
    *   `User` 및 `SemesterCourse` 엔티티와 관련 리포지토리 생성.
    *   `CourseController` 및 `CourseService`를 수정하여 로그인, 회원가입, 파일 업로드 시 데이터베이스 저장 및 조회 기능 추가.
    *   `application.properties`에 H2 파일 기반 데이터베이스 설정을 명시적으로 추가 (경로 변경, `AUTO_SERVER=TRUE`, `DB_CLOSE_ON_EXIT=FALSE`, `MVCC=TRUE`, `ddl-auto=update`, `generate-ddl=true`, `show-sql=true`, `format_sql=true`, `hibernate.temp.use_jdbc_metadata_defaults=false`).
    *   `index.html` 및 `upload-file.html` 웹 페이지 수정.
    *   콘솔 출력 인코딩 문제 해결을 위해 `build.gradle`에 JVM 인코딩 설정 추가.
    *   **현재 문제**: H2 데이터베이스 파일(`course_analyzer_db.mv.db`)이 지정된 경로(`C:/Users/하영욱/course_analyzer/temp/`)에 생성되지 않고, 서버 재시작 시 데이터가 사라지는 문제 지속. 콘솔 출력 문자 깨짐 현상 발생.