package models;

import java.time.LocalDateTime;

public class Employee {
    private final String name;
    private final LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    public Employee(String name, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        this.name = name;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
