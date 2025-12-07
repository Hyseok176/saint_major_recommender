package com.saintplus.transcript.dto;

public class SemesterInfo implements Comparable<SemesterInfo> {
    private int year;
    private String type; // "1", "2", "S", "W"
    private int comparableValue; // For chronological sorting

    public SemesterInfo(String rawSemester) {
        String[] parts = rawSemester.split("-");
        this.year = Integer.parseInt(parts[0]);
        this.type = parts[1];

        // Assign comparable value for sorting
        // 2021-1 -> 202101
        // 2021-S -> 202103 (after 1, before 2)
        // 2021-2 -> 202105
        // 2021-W -> 202107 (after 2)
        int typeValue;
        switch (type) {
            case "1": typeValue = 1; break;
            case "S": typeValue = 3; break;
            case "2": typeValue = 5; break;
            case "W": typeValue = 7; break;
            default: typeValue = 0; // Should not happen
        }
        this.comparableValue = this.year * 100 + typeValue;
    }

    public int getYear() { return year; }
    public String getType() { return type; }
    public int getComparableValue() { return comparableValue; }

    public boolean isRegularSemester() {
        return type.equals("1") || type.equals("2");
    }

    @Override
    public int compareTo(SemesterInfo other) {
        return Integer.compare(this.comparableValue, other.comparableValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemesterInfo that = (SemesterInfo) o;
        return year == that.year && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return 31 * year + type.hashCode();
    }

    @Override
    public String toString() {
        return year + "-" + type;
    }
}
