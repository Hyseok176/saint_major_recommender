package com.example.course_analyzer;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
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
    private UserRepository userRepository;

    @Autowired
    private SemesterCourseRepository semesterCourseRepository;

    @Autowired
    private CourseMappingRepository courseMappingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam(value = "nickname", required = false) String nickname,
                             @RequestParam(value = "email", required = false) String email) {
        User user = User.builder()
                .username(username)
                .nickname(nickname)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
        userRepository.save(user);
        return "redirect:/";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("major1") String major1,
                             @RequestParam("major2") String major2,
                             @RequestParam("major3") String major3,
                             Model model,
                             HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        String ipAddress = request.getRemoteAddr();
        try {
            courseService.updateUserTranscript(user, file, major1, major2, major3, ipAddress);
            return "redirect:/results";
        } catch (IOException e) {
            model.addAttribute("error", "Error processing file: " + e.getMessage());
            return "upload-file";
        }
    }

    @GetMapping("/results")
    public String showResults(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);

        if (savedCourses.isEmpty()) {
            return "redirect:/upload-form";
        }

        Map<Double, List<Course>> coursesBySemester = savedCourses.stream()
                .collect(Collectors.groupingBy(SemesterCourse::getSemester,
                        LinkedHashMap::new,
                        Collectors.mapping(sc -> {
                            String courseCode = sc.getCourseCode();
                            String actualCourseName = courseMappingRepository.findById(courseCode)
                                    .map(CourseMapping::getCourseName)
                                    .orElse(courseCode);
                            return new Course(String.valueOf(sc.getSemester()), courseCode, actualCourseName, sc.getGrade());
                        }, Collectors.toList())));

        model.addAttribute("coursesBySemester", coursesBySemester);
        return "results";
    }

    @GetMapping("/upload-form")
    public String showUploadForm() {
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String recommend(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);
        List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);
        double maxSemester = savedCourses.stream().mapToDouble(SemesterCourse::getSemester).max().orElse(0.0);
        Map<String, List<Course>> recommendedCourses = courseService.recommendCourses(user);
        model.addAttribute("title", "과목 추천");
        model.addAttribute("recommendedCourses", recommendedCourses);
        model.addAttribute("maxSemester", maxSemester);
        return "recommend";
    }

    @GetMapping("/all-courses")
    public String showAllCourses(@RequestParam(value = "major", required = false) String major, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = getUserFromAuthentication(authentication);

        List<String> userMajors = new ArrayList<>();
        if (user.getMajor1() != null && !user.getMajor1().isEmpty()) userMajors.add(user.getMajor1());
        if (user.getMajor2() != null && !user.getMajor2().isEmpty()) userMajors.add(user.getMajor2());
        if (user.getMajor3() != null && !user.getMajor3().isEmpty()) userMajors.add(user.getMajor3());
        model.addAttribute("userMajors", userMajors);

        if (major != null && !major.isEmpty() && !major.equals("All")) {
            String majorPrefix = getCoursePrefixForMajor(major);
            List<CourseStatDto> courses = courseService.getCoursesByMajor(majorPrefix);
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", major);
        } else {
            List<CourseMapping> courses = courseService.getAllCourses();
            model.addAttribute("courses", courses);
            model.addAttribute("selectedMajor", "All");
        }

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
            case "교육문화": return "EDU";
            default: return "";
        }
    }

    @GetMapping("/api/course-stats/{subjectCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCourseStats(@PathVariable String subjectCode) {
        try {
            Map<String, Object> stats = courseService.getCourseStats(subjectCode);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
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

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String providerId = oauth2User.getName();
            return userRepository.findByProviderAndProviderId("kakao", providerId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with providerId: " + providerId));
        } else {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        }
    }
}
