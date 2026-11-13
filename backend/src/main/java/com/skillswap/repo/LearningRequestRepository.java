package com.skillswap.repo;

import com.skillswap.model.LearningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LearningRequestRepository extends JpaRepository<LearningRequest, Long> {
    List<LearningRequest> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId);
}
