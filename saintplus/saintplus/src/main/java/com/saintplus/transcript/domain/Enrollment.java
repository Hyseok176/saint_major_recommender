package com.saintplus.transcript.domain;

import com.saintplus.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
//@Getter
//@ToString(exclude = "user")
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor
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
