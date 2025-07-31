package com.cognizant.vibe.synthetictesting.check;

import com.cognizant.vibe.synthetictesting.check.entity.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {
    /**
     * Finds all CheckResult entities for a given AppTarget ID, fetching the associated command to avoid N+1 queries.
     * The results are ordered by the most recent timestamp first.
     *
     * @param targetId The ID of the parent AppTarget.
     * @return A list of CheckResult entities.
     */
    @Query("SELECT r FROM CheckResult r JOIN FETCH r.command cmd WHERE cmd.app.id = :targetId ORDER BY r.timestamp DESC")
    List<CheckResult> findResultsForTarget(@Param("targetId") Long targetId);
}