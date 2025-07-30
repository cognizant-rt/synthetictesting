package com.cognizant.vibe.synthetictesting.check;

import com.cognizant.vibe.synthetictesting.check.entity.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {
}