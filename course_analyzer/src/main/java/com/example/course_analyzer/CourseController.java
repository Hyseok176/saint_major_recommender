package com.example.course_analyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        Model model,
                        RedirectAttributes redirectAttributes) {
        System.out.println("로그인 시도: 사용자 이름 = " + username); // 콘솔 출력
        User user = userRepository.findById(username).orElse(null);
        if (user != null && user.getPassword().equals(password)) { // 간단한 비밀번호 확인
            System.out.println("로그인 성공: 사용자 ID = " + user.getId() + ", 사용자 이름 = " + username); // 콘솔 출력
            redirectAttributes.addAttribute("userId", user.getId()); // userId 대신 user.getId() 사용
            // 이전에 저장된 학기별 교과목 정보가 있는지 확인
            List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);
            if (!savedCourses.isEmpty()) {
                System.out.println("이전 분석 결과가 존재하여 /results 페이지로 리디렉션합니다."); // 콘솔 출력
                return "redirect:/results"; // 있으면 바로 결과 페이지로 리디렉션
            } else {
                model.addAttribute("userId", user.getId());
                System.out.println("이전 분석 결과가 없어 /upload-file 페이지로 이동합니다."); // 콘솔 출력
                return "upload-file"; // 없으면 파일 업로드 페이지로
            }
        } else {
            System.out.println("로그인 실패: 유효하지 않은 사용자 이름 또는 비밀번호입니다."); // 콘솔 출력
            model.addAttribute("error", "유효하지 않은 사용자 이름 또는 비밀번호입니다.");
            return "index"; // 로그인 실패 시 다시 로그인 페이지로
        }
    }

    @PostMapping("/register") // 회원가입 처리 (간단한 예시)
    public String register(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           Model model) {
        System.out.println("회원가입 시도: 사용자 이름 = " + username + ", 비밀번호 = " + password); // 콘솔 출력
        if (userRepository.findById(username).isPresent()) {
            System.out.println("회원가입 실패: 이미 존재하는 사용자 이름입니다."); // 콘솔 출력
            model.addAttribute("error", "이미 존재하는 사용자 이름입니다.");
            return "index";
        }
        User newUser = new User();
        
        newUser.setPassword(password);
        newUser.setId(username); // 학번을 ID로 설정
        Long maxOrder = userRepository.findMaxUserOrder();
        newUser.setUserOrder(maxOrder != null ? maxOrder + 1 : 1L);
        userRepository.save(newUser);
        System.out.println("회원가입 성공: 사용자 ID = " + newUser.getId() + ", 사용자 이름 = " + username); // 콘솔 출력
        model.addAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "index";
    }


    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("userId") String userId, // userId를 String 타입으로 변경
            @RequestParam("file") MultipartFile file,
            Model model,
            RedirectAttributes redirectAttributes) {
        System.out.println("파일 업로드 시도: 사용자 ID = " + userId + ", 파일 이름 = " + file.getOriginalFilename()); // 콘솔 출력
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
            List<Course> rawCourses = courseService.analyzeFile(file.getInputStream());
            Map<String, List<Course>> coursesBySemester = courseService.groupAndFormatCourses(rawCourses);

            // 기존 saveAnalyzedCourses 대신 데이터베이스에 저장하는 로직 호출
            courseService.saveCoursesToDatabase(user, coursesBySemester);
            System.out.println("파일 분석 및 데이터베이스 저장 완료."); // 콘솔 출력

            redirectAttributes.addAttribute("userId", userId);
            return "redirect:/results";
        } catch (IOException e) {
            System.err.println("파일 처리 중 오류 발생: " + e.getMessage()); // 콘솔 에러 출력
            model.addAttribute("userId", userId);
            model.addAttribute("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "upload-file"; // Stay on upload page with error
        }
    }

    @GetMapping("/results")
    public String showResults(@RequestParam("userId") String userId, Model model) {
        System.out.println("/results 페이지 접근: 사용자 ID = " + userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));
        List<SemesterCourse> savedCourses = semesterCourseRepository.findByUser(user);

        if (savedCourses.isEmpty()) {
            System.out.println("저장된 데이터가 없어 /upload-form 페이지로 리디렉션합니다.");
            return "redirect:/upload-form?userId=" + userId;
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

        model.addAttribute("userId", userId);
        model.addAttribute("coursesBySemester", sortedCoursesBySemester);
        System.out.println("/results 페이지에 데이터 전달 완료.");
        return "results";
    }

    @GetMapping("/upload-form")
    public String showUploadForm(@RequestParam("userId") String userId, Model model) {
        model.addAttribute("userId", userId);
        return "upload-file";
    }

    @GetMapping("/recommend")
    public String recommend(@RequestParam("userId") String userId, Model model) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
        
        // 추천 로직 추가
        Map<String, List<Course>> recommendedCourses = courseService.recommendCourses(user);

        model.addAttribute("userId", userId);
        model.addAttribute("recommendedCourses", recommendedCourses);
        
        return "recommend";
    }
}
