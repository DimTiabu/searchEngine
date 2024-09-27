package searchengine.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query("SELECT s FROM SiteEntity s WHERE s.url = :url")
    SiteEntity findByUrl(String url);

    List<SiteEntity> findByStatus(SiteStatus status);
    @Transactional
    @Modifying
    @Query("DELETE FROM SiteEntity s WHERE s.url = :url")
    void deleteByUrl(String url);
}
