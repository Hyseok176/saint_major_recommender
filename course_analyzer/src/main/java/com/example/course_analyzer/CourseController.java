package com.example.course_analyzer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.security.Principal;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/register") // 회원가입 처리 (간단한 예시)
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) { // Model 대신 RedirectAttributes 사용
        if (userRepository.findById(username).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "이미 존재하는 사용자 이름입니다.");
            return "redirect:/";
        }
        User newUser = new User();
        
        newUser.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화
        newUser.setId(username); // 학번을 ID로 설정
        Long maxOrder = userRepository.findMaxUserOrder();
        newUser.setUserOrder(maxOrder != null ? maxOrder + 1 : 1L);
        userRepository.save(newUser);

        // 로그 출력
        String ip = request.getRemoteAddr();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println(String.format("Register: %s, User: %s, IP: %s", now, newUser.getId(), ip));

        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/"; // 루트로 리다이렉트
    }


    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("major1") String major1,
                             @RequestParam("major2") String major2,
                             @RequestParam("major3") String major3,
                             Principal principal,
                             Model model,
                             HttpServletRequest request, // Added HttpServletRequest
                             RedirectAttributes redirectAttributes) {
        String userId = principal.getName();
        String ipAddress = request.getRemoteAddr(); // Get IP address
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
            user.setMajor1(major1.replace(" ", "")); 
            user.setMajor2(major2.replace(" ", ""));
            user.setMajor3(major3.replace(" ", ""));
            userRepository.save(user); // Save the updated user
            // Pass userId and ipAddress to analyzeFile
            Map<String, Object> analysisResult = courseService.analyzeFile(file.getInputStream(), userId, ipAddress);
            List<Course> rawCourses = (List<Course>) analysisResult.get("rawCourses");
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
    public String showResults(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        String userId = principal.getName();
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
    public String showUploadForm(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        // This method now implicitly requires authentication due to SecurityConfig
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String recommend(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        String userId = principal.getName();
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

    @GetMapping("/all-courses")
    public String showAllCourses(@RequestParam(value = "major", required = false) String major,
                                 Principal principal, Model model, RedirectAttributes redirectAttributes) {
        String userId = principal.getName();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

        // Get user's majors for the dropdown, filtering out empty or "미선택" values
        List<String> userMajors = new java.util.ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty() && !user.getMajor1().equals("미선택")) {
            userMajors.add(user.getMajor1());
        }
        if (user.getMajor2() != null && !user.getMajor2().isEmpty() && !user.getMajor2().equals("미선택")) {
            userMajors.add(user.getMajor2());
        }
        if (user.getMajor3() != null && !user.getMajor3().isEmpty() && !user.getMajor3().equals("미선택")) {
            userMajors.add(user.getMajor3());
        }
        model.addAttribute("userMajors", userMajors);

        String selectedMajor = "All"; // Default

        if (major != null && !major.isEmpty() && !major.equals("All")) {
            // Filter by the selected major
            String majorPrefix = getCoursePrefixForMajor(major);
            List<CourseStatDto> courses = courseService.getCoursesByMajor(majorPrefix);
            model.addAttribute("courses", courses);
            selectedMajor = major;
        } else {
            // Show all courses by default or when "All" is selected
            List<CourseMapping> courses = courseService.getAllCourses();
            model.addAttribute("courses", courses);
        }

        model.addAttribute("selectedMajor", selectedMajor);
        return "all-courses";
    }

    private String getCoursePrefixForMajor(String major) {
        switch (major) {
            case "수학": return "MAT";
            case "물리학": return "PHY";
            case "화학": return "CHM";
            case "생명과학": return "BIO";
            case "전자공학": return "EEE";
            case "기계공학": return "MEE";
            case "컴퓨터공학": return "CSE";
            case "화공생명공학": return "CBE";
            case "시스템반도체공학": return "SSE";
            case "인공지능학과": return "AIE";
            case "경제학": return "ECO";
            case "경영학": return "MGT";
            default: return ""; // Should not happen if "미선택" is handled
        }
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
