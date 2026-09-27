package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Integer> {
    Optional<Publication> findByDoi(String doi);

    Optional<Publication> findByTitleIgnoreCaseAndPublicationYear(String title, Integer publicationYear);

    Optional<Publication> findByIdAndStatus(Integer id, PublicationStatus status);

    Optional<Publication> findByIdAndMembers_Id(Integer id, Integer memberId);

    Page<Publication> findByMembers_Id(Integer memberId, Pageable pageable);

    List<Publication> findByMembers_IdOrderByPublicationYearDescCreatedAtDesc(Integer memberId);

    List<Publication> findByPublicationYear(Integer publicationYear);

    List<Publication> findTop200ByOrderByCreatedAtDesc();

    List<Publication> findByStatusOrderByPublicationYearDescTitleAsc(PublicationStatus status);

    @Query("""
        SELECT p FROM Publication p
        WHERE p.status = :status
          AND (:search = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:author = '' OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:year IS NULL OR p.publicationYear = :year)
          AND (:type IS NULL OR p.type = :type)
    """)
    Page<Publication> searchPublications(
            @Param("search") String search,
            @Param("author") String author,
            @Param("year") Integer year,
            @Param("type") PublicationType type,
            @Param("status") PublicationStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Publication p JOIN p.members m
        WHERE m.id = :memberId
          AND p.status = :status
          AND (:search = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:author = '' OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:year IS NULL OR p.publicationYear = :year)
          AND (:type IS NULL OR p.type = :type)
    """)
    Page<Publication> searchMemberPublications(
            @Param("memberId") Integer memberId,
            @Param("search") String search,
            @Param("author") String author,
            @Param("year") Integer year,
            @Param("type") PublicationType type,
            @Param("status") PublicationStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Publication p JOIN p.members m
        WHERE m.id = :memberId
          AND (:status IS NULL OR p.status = :status)
          AND (:search = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.doi, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:year IS NULL OR p.publicationYear = :year)
          AND (:type IS NULL OR p.type = :type)
    """)
    Page<Publication> searchDashboardPublicationsForMember(
            @Param("memberId") Integer memberId,
            @Param("search") String search,
            @Param("year") Integer year,
            @Param("type") PublicationType type,
            @Param("status") PublicationStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "members")
    @Query(value = """
        SELECT DISTINCT p FROM Publication p LEFT JOIN p.members m
        WHERE (:memberId IS NULL OR m.id = :memberId)
          AND (:status IS NULL OR p.status = :status)
          AND (:search = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.doi, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:year IS NULL OR p.publicationYear = :year)
          AND (:type IS NULL OR p.type = :type)
        """, countQuery = """
        SELECT COUNT(DISTINCT p.id) FROM Publication p LEFT JOIN p.members m
        WHERE (:memberId IS NULL OR m.id = :memberId)
          AND (:status IS NULL OR p.status = :status)
          AND (:search = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.authorsText, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(p.doi, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:year IS NULL OR p.publicationYear = :year)
          AND (:type IS NULL OR p.type = :type)
        """)
    Page<Publication> searchValidationQueue(
            @Param("memberId") Integer memberId,
            @Param("search") String search,
            @Param("year") Integer year,
            @Param("type") PublicationType type,
            @Param("status") PublicationStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p.publicationYear FROM Publication p
        WHERE p.status = :status AND p.publicationYear IS NOT NULL
        ORDER BY p.publicationYear DESC
    """)
    List<Integer> findDistinctPublicationYearsByStatus(@Param("status") PublicationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM publication_validation_history
            WHERE publication_id IN (:publicationIds)
              AND NOT EXISTS (
                  SELECT 1
                  FROM members_publications mp
                  WHERE mp.publication_id = publication_validation_history.publication_id
              )
            """, nativeQuery = true)
    int deleteValidationHistoryForOrphanPublications(@Param("publicationIds") List<Integer> publicationIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM publications
            WHERE id IN (:publicationIds)
              AND NOT EXISTS (
                  SELECT 1
                  FROM members_publications mp
                  WHERE mp.publication_id = publications.id
              )
            """, nativeQuery = true)
    int deleteOrphanPublicationsByIds(@Param("publicationIds") List<Integer> publicationIds);
}