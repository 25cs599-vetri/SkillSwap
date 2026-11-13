package com.skillswap.controller;

import com.skillswap.model.*;
import com.skillswap.repo.*;
import com.skillswap.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StudentRepository students;
    private final SkillRepository skills;
    private final LearningRequestRepository requests;
    private final AuthService auth;
    private final CurrentStudent current;

    public ApiController(StudentRepository students, SkillRepository skills,
                         LearningRequestRepository requests, AuthService auth, CurrentStudent current) {
        this.students = students; this.skills = skills; this.requests = requests;
        this.auth = auth; this.current = current;
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "SkillSwap API is running"); }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        var result = auth.register(body.get("name"), body.get("email"), body.get("password"));
        return authResponse(result);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return authResponse(auth.login(body.get("email"), body.get("password")));
    }

    private Map<String, Object> authResponse(AuthService.AuthResult result) {
        return Map.of("token", result.token(), "student", studentDto(result.student(), true));
    }

    @GetMapping("/me")
    public Map<String, Object> me() { return studentDto(current.get(), true); }

    @PutMapping("/me")
    public Map<String, Object> updateMe(@RequestBody Map<String, String> body) {
        Student s = current.get();
        s.setCollege(body.getOrDefault("college", ""));
        s.setDepartment(body.getOrDefault("department", ""));
        s.setBio(body.getOrDefault("bio", ""));
        students.save(s);
        return studentDto(s, true);
    }

    @GetMapping("/skills")
    public List<Map<String, Object>> mySkills() {
        return skills.findByStudentId(current.get().getId()).stream().map(this::skillDto).toList();
    }

    @PostMapping("/skills")
    public Map<String, Object> addSkill(@RequestBody Map<String, String> body) {
        Student me = current.get();
        String name = body.getOrDefault("skillName", "").trim();
        Skill.SkillType type;
        try { type = Skill.SkillType.valueOf(body.getOrDefault("skillType", "").toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Choose TEACH or LEARN."); }
        if (name.isBlank() || name.length() > 60) throw new IllegalArgumentException("Enter a skill name (maximum 60 characters).");
        if (skills.existsByStudentIdAndSkillNameIgnoreCaseAndSkillType(me.getId(), name, type))
            throw new IllegalArgumentException("That skill is already in your list.");
        Skill skill = new Skill(); skill.setStudent(me); skill.setSkillName(name); skill.setSkillType(type);
        return skillDto(skills.save(skill));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Long id) {
        Skill skill = skills.findById(id).orElseThrow(() -> new IllegalArgumentException("Skill not found."));
        if (!skill.getStudent().getId().equals(current.get().getId())) throw new IllegalArgumentException("You can only remove your own skills.");
        skills.delete(skill);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students")
    public List<Map<String, Object>> discover() {
        Long me = current.get().getId();
        return students.findAll().stream().filter(s -> !s.getId().equals(me))
                .map(s -> studentDto(s, false)).toList();
    }

    @GetMapping("/matches")
    public List<Map<String, Object>> matches() {
        Student me = current.get();
        Set<String> iCanTeach = skillNames(me.getId(), Skill.SkillType.TEACH);
        Set<String> iWant = skillNames(me.getId(), Skill.SkillType.LEARN);
        return students.findAll().stream().filter(s -> !s.getId().equals(me.getId())).map(other -> {
            Set<String> theyTeach = skillNames(other.getId(), Skill.SkillType.TEACH);
            Set<String> theyWant = skillNames(other.getId(), Skill.SkillType.LEARN);
            Set<String> useful = new TreeSet<>(iWant); useful.retainAll(theyTeach);
            Set<String> reciprocal = new TreeSet<>(iCanTeach); reciprocal.retainAll(theyWant);
            Map<String, Object> dto = studentDto(other, false);
            dto.put("matchSkills", useful);
            dto.put("reciprocalSkills", reciprocal);
            dto.put("matchScore", useful.size() + reciprocal.size());
            return dto;
        }).filter(m -> (Integer)m.get("matchScore") > 0)
          .sorted((a,b) -> Integer.compare((Integer)b.get("matchScore"), (Integer)a.get("matchScore")))
          .toList();
    }

    @PostMapping("/requests")
    public Map<String, Object> sendRequest(@RequestBody Map<String, Object> body) {
        Student sender = current.get();
        Long receiverId = Long.valueOf(String.valueOf(body.get("receiverId")));
        if (sender.getId().equals(receiverId)) throw new IllegalArgumentException("You cannot send a request to yourself.");
        Student receiver = students.findById(receiverId).orElseThrow(() -> new IllegalArgumentException("Student not found."));
        String skill = String.valueOf(body.getOrDefault("skill", "")).trim();
        if (skill.isBlank()) throw new IllegalArgumentException("Choose a skill to learn.");
        LearningRequest r = new LearningRequest();
        r.setSender(sender); r.setReceiver(receiver); r.setSkill(skill);
        r.setMessage(String.valueOf(body.getOrDefault("message", "")));
        return requestDto(requests.save(r), sender.getId());
    }

    @GetMapping("/requests")
    public List<Map<String, Object>> myRequests() {
        Long id = current.get().getId();
        return requests.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(id, id)
                .stream().map(r -> requestDto(r, id)).toList();
    }

    @PutMapping("/requests/{id}/status")
    public Map<String, Object> updateRequest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Student me = current.get();
        LearningRequest r = requests.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found."));
        String status = body.getOrDefault("status", "").toUpperCase();
        if (r.getReceiver().getId().equals(me.getId()) && (status.equals("ACCEPTED") || status.equals("REJECTED"))) {
            r.setStatus(LearningRequest.RequestStatus.valueOf(status));
        } else if (r.getSender().getId().equals(me.getId()) && status.equals("COMPLETED")
                && r.getStatus() == LearningRequest.RequestStatus.ACCEPTED) {
            r.setStatus(LearningRequest.RequestStatus.COMPLETED);
        } else {
            throw new IllegalArgumentException("You are not allowed to make that status change.");
        }
        return requestDto(requests.save(r), me.getId());
    }

    private Set<String> skillNames(Long studentId, Skill.SkillType type) {
        return skills.findByStudentId(studentId).stream().filter(s -> s.getSkillType() == type)
                .map(s -> s.getSkillName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    private Map<String, Object> studentDto(Student s, boolean includeEmail) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", s.getId()); dto.put("name", s.getName());
        dto.put("college", s.getCollege()); dto.put("department", s.getDepartment()); dto.put("bio", s.getBio());
        dto.put("skills", skills.findByStudentId(s.getId()).stream().map(this::skillDto).toList());
        if (includeEmail) dto.put("email", s.getEmail());
        return dto;
    }

    private Map<String, Object> skillDto(Skill s) {
        return Map.of("id", s.getId(), "skillName", s.getSkillName(), "skillType", s.getSkillType().name());
    }

    private Map<String, Object> requestDto(LearningRequest r, Long viewerId) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", r.getId()); dto.put("skill", r.getSkill()); dto.put("message", r.getMessage());
        dto.put("status", r.getStatus().name()); dto.put("createdAt", r.getCreatedAt().toString());
        dto.put("senderId", r.getSender().getId()); dto.put("senderName", r.getSender().getName());
        dto.put("receiverId", r.getReceiver().getId()); dto.put("receiverName", r.getReceiver().getName());
        dto.put("direction", r.getSender().getId().equals(viewerId) ? "SENT" : "RECEIVED");
        return dto;
    }
}
