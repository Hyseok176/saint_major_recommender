package com.example.course_analyzer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserRepository userRepository; // UserRepository 주입
    @Autowired
    private SemesterCourseRepository semesterCourseRepository; // SemesterCourseRepository 주입

    @Autowired
    private CourseMappingRepository courseMappingRepository; // CourseMappingRepository 주입

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/login") // 로그인 처리
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        HttpServletRequest request,
                        Model model) {
        User user = userRepository.findById(username).orElse(null);
        if (user != null && user.getPassword().equals(password)) { // 간단한 비밀번호 확인
            session.setAttribute("userId", user.getId()); // 세션에 userId 저장

            // 로그 출력
            String ip = request.getRemoteAddr();
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println(String.format("Login: %s, User: %s, IP: %s", now, user.getId(), ip));

            // 이전에 저장된 학기별 교과목 정보가 있는지 확인
            List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);
            if (!savedCourses.isEmpty()) {
                return "redirect:/results"; // 있으면 바로 결과 페이지로 리디렉션
            } else {
                return "redirect:/upload-form"; // 없으면 파일 업로드 페이지로
            }
        } else {
            model.addAttribute("error", "Invalid username or password.");
            return "index"; // 로그인 실패 시 다시 로그인 페이지로
        }
    }

    @PostMapping("/register") // 회원가입 처리 (간단한 예시)
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           HttpServletRequest request,
                           Model model) {
        if (userRepository.findById(username).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "index";
        }
        User newUser = new User();
        
        newUser.setPassword(password);
        newUser.setId(username); // 학번을 ID로 설정
        Long maxOrder = userRepository.findMaxUserOrder();
        newUser.setUserOrder(maxOrder != null ? maxOrder + 1 : 1L);
        userRepository.save(newUser);

        // 로그 출력
        String ip = request.getRemoteAddr();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(String.format("Register: %s, User: %s, IP: %s", now, newUser.getId(), ip));

        model.addAttribute("message", "회원가입이 완료되었습니다.");
        return "index";
    }


    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("major1") String major1,
                             @RequestParam("major2") String major2,
                             @RequestParam("major3") String major3,
                             HttpSession session,
                             Model model,
                             HttpServletRequest request, // Added HttpServletRequest
                             RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/"; // 로그인되지 않은 경우 로그인 페이지로
        }
        String ipAddress = request.getRemoteAddr(); // Get IP address
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
            user.setMajor1(major1); 
            user.setMajor2(major2);
            user.setMajor3(major3);
            userRepository.save(user); // Save the updated user
            // Pass userId and ipAddress to analyzeFile
            Map<String, Object> analysisResult = courseService.analyzeFile(file.getInputStream(), userId, ipAddress);
            List<Course> rawCourses = (List<Course>) analysisResult.get("rawCourses");
            // Map<String, String> majorInfo = (Map<String, String>) analysisResult.get("majorInfo"); // 파일에서 전공 파싱 로직 제거

            // 전공 정보 저장 (기존 로직 유지, majorInfo가 파일에서 파싱된 경우)
            // if (majorInfo != null) {
            //     user.setMajor1(majorInfo.get("major1"));
            //     user.setMajor2(majorInfo.get("major2"));
            //     user.setMajor3(majorInfo.get("major3"));
            //     userRepository.save(user); // User 엔티티 업데이트
            // }
            Map<String, List<Course>> coursesBySemester = courseService.groupAndFormatCourses(rawCourses);

            // 기존 saveAnalyzedCourses 대신 데이터베이스에 저장하는 로직 호출
            courseService.saveCoursesToDatabase(user, coursesBySemester);

            return "redirect:/results";
        } catch (IOException e) {
            model.addAttribute("error", "Error processing file: " + e.getMessage());
            return "upload-file"; // Stay on upload page with error
        }
    }

    @GetMapping("/results")
    public String showResults(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/"; // 로그인되지 않은 경우 로그인 페이지로
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
        List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);

        if (savedCourses.isEmpty()) {
            return "redirect:/upload-form";
        }

        Map<Double, List<Course>> coursesBySemester = savedCourses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester,
                        LinkedHashMap::new, // 순서 유지를 위해 LinkedHashMap 사용
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode(); // SEMESTER_COURSE에는 과목 코드가 저장되어 있음
                            String actualCourseName = courseMappingRepository.findById(courseCode)
                                    .map(CourseMapping::getCourseName)
                                    .orElse(courseCode); // 매핑된 이름이 없으면 과목 코드를 그대로 사용
                            return new Course(String.valueOf(sc.getSemester()), courseCode, actualCourseName, sc.getGrade());
                        }, Collectors.toList())));

        // 학기 순서 정렬
        Map<Double, List<Course>> sortedCoursesBySemester = coursesBySemester.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));

        model.addAttribute("coursesBySemester", sortedCoursesBySemester);
        return "results";
    }

    @GetMapping("/upload-form")
    public String showUploadForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/"; // 로그인되지 않은 경우 로그인 페이지로
        }
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String recommend(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/"; // 로그인되지 않은 경우 로그인 페이지로
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));

        List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);

        double maxSemester = savedCourses.stream()
                .mapToDouble(SemesterCourse::getSemester)
                .max()
                .orElse(0.0);

        // 추천 로직 추가
        Map<String, List<Course>> recommendedCourses = courseService.recommendCourses(user);

        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCourses", recommendedCourses);
        model.addAttribute("maxSemester", maxSemester);

        return "recommend";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/all-courses")
    public String showAllCourses(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/"; // 로그인되지 않은 경우 로그인 페이지로
        }
        List<CourseMapping> allCourses = courseService.getAllCourses();
        model.addAttribute("courses", allCourses);
        return "all-courses";
    }

    @GetMapping("/api/course-stats/{subjectCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourseStats(@PathVariable String subjectCode) {
        try {
            Map<String, Object> stats = courseService.getCourseStats(subjectCode);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching course stats for " + subjectCode + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/extract-majors")
    @ResponseBody
    public ResponseEntity<List<String>> extractMajors(@RequestParam("file") MultipartFile file) {
        try {
            List<String> majors = courseService.extractMajorsFromFile(file.getInputStream());
            return ResponseEntity.ok(majors);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
