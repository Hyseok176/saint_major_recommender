package com.saintplus.course.service;

import com.saintplus.course.domain.Course;
import com.saintplus.course.repository.CourseRepository;
import com.saintplus.transcript.repository.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * CourseService 테스트
 * 
 * 과목 관련 비즈니스 로직의 정상 작동을 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    @DisplayName("전체 과목 조회 - 성공")
    void testGetAllCourses() {
        // Given
        Course course1 = new Course("CSE2010", "자료구조");
        Course course2 = new Course("CSE3010", "알고리즘");
        List<Course> mockCourses = List.of(course1, course2);

        when(courseRepository.findAll()).thenReturn(mockCourses);

        // When
        List<Course> result = courseService.getAllCourses();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCourseCode()).isEqualTo("CSE2010");
        assertThat(result.get(1).getCourseCode()).isEqualTo("CSE3010");
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("전공 코드 변환 - 컴퓨터공학")
    void testGetCoursePrefixForMajor_ComputerScience() {
        // When
        String prefix = courseService.getCoursePrefixForMajor("컴퓨터공학");

        // Then
        assertThat(prefix).isEqualTo("CSE");
    }

    @Test
    @DisplayName("전공 코드 변환 - 수학")
    void testGetCoursePrefixForMajor_Math() {
        // When
        String prefix = courseService.getCoursePrefixForMajor("수학");

        // Then
        assertThat(prefix).isEqualTo("MAT");
    }

    @Test
    @DisplayName("전공 코드 변환 - 알 수 없는 전공")
    void testGetCoursePrefixForMajor_Unknown() {
        // When
        String prefix = courseService.getCoursePrefixForMajor("알수없는전공");

        // Then
        assertThat(prefix).isEmpty();
    }
}
