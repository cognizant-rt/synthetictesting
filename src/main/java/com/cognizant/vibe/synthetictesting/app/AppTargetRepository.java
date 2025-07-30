package com.cognizant.vibe.synthetictesting.app;

import com.cognizant.vibe.synthetictesting.app.entity.AppTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppTargetRepository extends JpaRepository<AppTarget, Long> {
    /**
     * Finds all AppTarget entities with the specified enabled status.
     *
     * @param enabled The desired status (true for enabled, false for disabled).
     * @return A list of matching AppTarget entities.
     */
    List<AppTarget> findByEnabled(boolean enabled);
}