# Project Improvement Recommendations

This document outlines potential improvements for the web application, based on an analysis of its current state. The recommendations cover security, architecture, UI/UX, database design, and error handling.

### 1. Security (Highest Priority)

*   **Problem:** User passwords are currently stored and compared in plain text, which is a critical security vulnerability. If the database is compromised, all user passwords will be exposed.
*   **Recommendation:**
    *   **Password Encryption:** Integrate **Spring Security** and use a strong hashing algorithm like `BCryptPasswordEncoder` to encrypt passwords before storing them. Authentication should be performed by comparing the user's input with the stored hash.

### 2. Backend Architecture

*   **Problem:** The `CourseController` is a "fat controller," handling too many responsibilities, including business logic for authentication, file processing, and course filtering.
*   **Recommendation:**
    *   **Separation of Concerns:** Refactor the controller to delegate business logic to the `CourseService`. For instance, logic for fetching user majors and filtering courses should be moved to the service layer to improve code cohesion and reusability.

*   **Problem:** The mapping between major names (e.g., "컴퓨터공학") and course code prefixes (e.g., "CSE") is hardcoded in the `getCoursePrefixForMajor` method. This requires code changes and redeployment for any new majors.
*   **Recommendation:**
    *   **Database-Driven Mapping:** Store this mapping in a dedicated database table (e.g., `MajorInfo`). This would allow administrators to add or modify majors without changing the application code.

### 3. Frontend & UI/UX

*   **Problem:** CSS and JavaScript code is embedded directly within HTML files, and common UI elements (like the logo and logout button) are duplicated across multiple pages.
*   **Recommendation:**
    *   **Static File Separation:** Externalize CSS and JavaScript into separate `.css` and `.js` files for better maintainability.
    *   **Template Inheritance:** Use Thymeleaf's layout/fragment features (`th:replace`, `th:insert`) to create a common template for shared UI elements like headers and footers. This will significantly reduce code duplication.

*   **Problem:** There is a lack of visual feedback (e.g., loading spinners) during time-consuming operations like file uploads or data processing.
*   **Recommendation:**
    *   **Loading Indicators:** Implement loading spinners or disable buttons during asynchronous operations to improve the user experience by clearly indicating that a process is underway.

### 4. Database Design

*   **Problem:** The `User` entity stores major information in three separate string fields (`major1`, `major2`, `major3`), which violates normalization principles and can lead to data inconsistency (e.g., "컴퓨터공학" vs. "컴퓨터 공학").
*   **Recommendation:**
    *   **Data Normalization:** Create a separate `Major` table and establish a many-to-many relationship between `User` and `Major`. This ensures data integrity and allows for more flexible and powerful queries.

### 5. Error Handling

*   **Problem:** The application displays generic error messages (e.g., "Whitelabel Error Page," "Error processing file") for both server-side issues and invalid user input.
*   **Recommendation:**
    *   **User-Friendly Error Pages:** Create custom error pages for common HTTP status codes (e.g., 404 Not Found, 500 Internal Server Error) to provide more helpful guidance to the user.
    *   **Specific Error Messages:** When a file parsing error occurs, use `RedirectAttributes` to pass specific, informative messages to the frontend (e.g., "Unsupported file format" or "The file content is invalid"). This helps users resolve issues on their own.