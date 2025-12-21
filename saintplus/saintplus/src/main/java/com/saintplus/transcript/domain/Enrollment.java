package com.saintplus.transcript.domain;

import com.saintplus.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semester_course")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double semester;

    @Column(nullable = false)
    private String courseCode;

    @ManyToOne
    private User user;

    @Embedded
    private Remarks importantRemarks;

}
