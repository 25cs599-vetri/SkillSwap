package com.skillswap.service;

import com.skillswap.model.Student;
import com.skillswap.repo.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(StudentRepository students, PasswordEncoder encoder, TokenService tokens) {
        this.students = students;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public AuthResult register(String name, String email, String password) {
        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.length() < 8)
            throw new IllegalArgumentException("Enter a name, valid email, and password with at least 8 characters.");
        if (students.existsByEmailIgnoreCase(email.trim()))
            throw new IllegalArgumentException("An account with this email already exists.");
        Student s = new Student();
        s.setName(name.trim());
        s.setEmail(email.trim().toLowerCase());
        s.setPassword(encoder.encode(password));
        students.save(s);
        return new AuthResult(tokens.create(s.getId()), s);
    }

    public AuthResult login(String email, String password) {
        Student s = students.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (password == null || !encoder.matches(password, s.getPassword()))
            throw new IllegalArgumentException("Invalid email or password.");
        return new AuthResult(tokens.create(s.getId()), s);
    }

    public record AuthResult(String token, Student student) {}
}
