package com.cognizant.vibe.synthetictesting.check;

import com.cognizant.vibe.synthetictesting.check.entity.CheckCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckCommandRepository extends JpaRepository<CheckCommand, Long> {
    /**
     * Finds all CheckCommand entities associated with a specific AppTarget ID.
     * Spring Data JPA automatically derives the JPQL query from the method name.
     * The query will be equivalent to "SELECT c FROM CheckCommand c WHERE c.app.id = :appId".
     *
     * @param appId The ID of the parent AppTarget.
     * @return A list of associated CheckCommand entities.
     */
    List<CheckCommand> findByAppId(Long appId);


    /**
     * Finds all CheckCommand entities for a given AppTarget ID, and eagerly fetches the associated AppTarget entity
     * to prevent LazyInitializationException in background tasks.
     *
     * @param appId The ID of the parent AppTarget.
     * @return A list of CheckCommand entities with their 'app' property fully initialized.
     */
    @Query("SELECT c FROM CheckCommand c JOIN FETCH c.app WHERE c.app.id = :appId")
    List<CheckCommand> findByAppIdWithAppTarget(@Param("appId") Long appId);

}