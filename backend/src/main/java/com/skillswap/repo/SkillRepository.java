package com.skillswap.repo;

import com.skillswap.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByStudentId(Long studentId);
    boolean existsByStudentIdAndSkillNameIgnoreCaseAndSkillType(Long studentId, String skillName, Skill.SkillType skillType);
}
