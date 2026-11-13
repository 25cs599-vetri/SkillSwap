package com.skillswap.service;

import com.skillswap.model.Student;
import com.skillswap.repo.StudentRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentStudent {
    private Long id;
    private final StudentRepository students;

    public CurrentStudent(StudentRepository students) { this.students = students; }
    public void setId(Long id) { this.id = id; }
    public Student get() {
        if (id == null) throw new IllegalArgumentException("Please log in again.");
        return students.findById(id).orElseThrow(() -> new IllegalArgumentException("Student account not found."));
    }
}
