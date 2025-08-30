# Project Summary: Course Analyzer

## 1. Project Overview

This project aims to help university students plan their courses more effectively. Users can upload their course history file (a .txt file from the university's system), and the application will analyze, parse, and visualize the data on a web page. The system provides features like viewing past courses, receiving course recommendations, and planning for future semesters.

## 2. Core Features

*   **User Management**: Secure user registration and login (both local and Kakao OAuth2).
*   **Transcript Analysis**: Parses `.txt` transcript files to extract course history by semester.
*   **Visualized Results**: Displays analyzed course history grouped by semester, including regular and seasonal semesters.
*   **Course Recommendation Engine**:
    *   Recommends major and general education courses based on what the user hasn't taken.
    *   Uses a weighted scoring algorithm considering both the number of students who took the course and semester proximity.
*   **Future Semester Planning**:
    *   Allows users to add courses to a "cart" for future semesters.
    *   Planned semesters are displayed alongside completed ones for a comprehensive view.
*   **Course Catalog**:
    *   Provides a view of all available courses, filterable by major and semester.
    *   Displays statistics for each course, such as the number of students per semester.
*   **Crawler Management**:
    *   An admin page to manually trigger a Python-based web crawler.
    *   The crawler fetches the latest course catalog data from the university website.
    *   A scheduler automatically runs the crawler at regular intervals.

## 3. How to Run the Project

1.  **Environment**: JDK 11 or higher is required.
2.  **Execution**: Navigate to the project root directory and run the following command:
    ```bash
    ./gradlew bootRun
    ```
3.  **Access**: Open a web browser and go to `http://localhost:8080`.
4.  **Database Console**: The H2 database console is available at `http://localhost:8080/h2-console` for direct data inspection (credentials are in `application.properties`).

## 4. Technology Stack

*   **Backend**: Java, Spring Boot, Spring Security
*   **Frontend**: HTML, Thymeleaf, JavaScript
*   **Database**: H2 (In-memory/File-based)
*   **Crawler**: Python, Selenium, Pandas
*   **Build Tool**: Gradle

## 5. Key Logic & Architecture

*   **`CourseController.java`**: Handles all user-facing HTTP requests, such as rendering pages, processing form submissions (login, file upload), and managing API endpoints. It delegates business logic to the service layer.
*   **`CourseService.java`**: Contains the core business logic.
    *   **File Parsing**: Uses regular expressions to extract course information from the uploaded text file. It correctly handles regular and seasonal semesters.
    *   **Recommendation Logic**: Implements the weighted scoring algorithm for course recommendations.
    *   **Database Interaction**: Manages saving and retrieving user and course data via repositories.
*   **`CrawlerService.java`**: Manages the execution of the Python web crawler. It uses `ProcessBuilder` to run the Python script with appropriate parameters (year, semester) and is scheduled to run automatically via Spring's `@Scheduled` annotation.
*   **`sogang_crawler_gui.py`**: A Python script using Selenium to crawl the university's course catalog website. It downloads the course list (as an HTML file disguised as XLS), parses it using Pandas, and saves it as a clean CSV file for the main application to use.
*   **Security**: Spring Security is used to manage authentication and authorization. A custom `AuthenticationEntryPoint` handles unauthenticated access by redirecting to the login page with a warning message.

## 6. Database Model

*   **`User`**: Stores user information, including credentials and major details.
*   **`SemesterCourse`**: Stores each course a user has taken, linked to the user and including semester, course name, and grade.
*   **`CourseMapping`**: Maps course codes to full course names.
*   **`SavedCourse`**: Represents a course a user has saved to their "cart" for future planning.

## 7. Future Improvement Recommendations

*   **Security**: Encrypt user passwords using a strong hashing algorithm like `BCryptPasswordEncoder` (partially implemented with Spring Security).
*   **Architecture**:
    *   Refactor the hardcoded mapping of major names to course prefixes into a database table for easier management.
*   **Frontend & UI/UX**:
    *   Externalize CSS and JavaScript into separate static files.
    *   Use Thymeleaf fragments to create a common layout template and reduce code duplication in HTML files.
*   **Database Design**:
    *   Normalize the `User` entity by creating a separate `Major` table and a many-to-many relationship to avoid storing majors as simple strings.
*   **Error Handling**:
    *   Create user-friendly custom error pages for 404 and 500 errors.
*   **Code Quality**:
    *   Implement a structured logging strategy using SLF4J instead of `System.out.println`.
    *   Add unit and integration tests to ensure code reliability.