package com.skillswap.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_requests")
public class LearningRequest {
    public enum RequestStatus { PENDING, ACCEPTED, REJECTED, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false)
    private Student sender;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receiver_id", nullable = false)
    private Student receiver;

    @Column(nullable = false)
    private String skill;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Student getSender() { return sender; }
    public void setSender(Student sender) { this.sender = sender; }
    public Student getReceiver() { return receiver; }
    public void setReceiver(Student receiver) { this.receiver = receiver; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
