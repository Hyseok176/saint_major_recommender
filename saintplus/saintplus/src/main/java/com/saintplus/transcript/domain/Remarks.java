package com.saintplus.transcript.domain;

import jakarta.persistence.Embeddable;
import lombok.Setter;


@Embeddable
@Setter
public class Remarks {

    private boolean isEnglishLecture; // E 영어강의
    private boolean isRetake;         // R 재이수로성적취득 후 기존성적 대체
    private boolean isDuplicate;      // M 중복 인정 과목
    private boolean isFailed;         // U,F,FA 미이수 처리


    public Remarks() {}

    public Remarks(boolean englishLecture, boolean retake, boolean duplicate, boolean passFail) {
        this.isEnglishLecture = englishLecture;
        this.isRetake = retake;
        this.isDuplicate = duplicate;
        this.isFailed = passFail;
    }


    public boolean isEnglishLecture() { return isEnglishLecture; }
    public boolean isRetake() { return isRetake; }
    public boolean isDuplicate() { return isDuplicate; }
    public boolean isFailed() { return isFailed; }

}
