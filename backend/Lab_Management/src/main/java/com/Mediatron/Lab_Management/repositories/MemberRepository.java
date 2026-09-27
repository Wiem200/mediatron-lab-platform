package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {

    Optional<Member> findByUserId(Integer userId);

    Optional<Member> findByOrcidId(String orcidId);

    List<Member> findByLastNameContainingIgnoreCase(String lastName);

    List<Member> findAllByOrderByLastNameAscFirstNameAsc();

    @Query(value = """
            SELECT m.*
            FROM members m
            LEFT JOIN users u ON u.id = m.user_id
            WHERE u.id IS NULL
               OR NOT EXISTS (
                   SELECT 1
                   FROM user_roles ur
                   JOIN roles r ON r.id = ur.role_id
                   WHERE ur.user_id = u.id
                     AND r.name = 'ROLE_ADMIN'
               )
            ORDER BY LOWER(COALESCE(m.last_name, '')) ASC, LOWER(COALESCE(m.first_name, '')) ASC
            """, nativeQuery = true)
    List<Member> findVisibleLabMembersByOrderByName();

    @Query("""
            SELECT m
            FROM Member m
            JOIN m.user u
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<Member> findByUserEmail(@Param("email") String email);

    @Query("""
            SELECT m
            FROM Member m
            WHERE LOWER(m.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<Member> searchByName(@Param("query") String query);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM member_research_axes WHERE member_id = :memberId", nativeQuery = true)
    int deleteResearchAxesByMemberId(@Param("memberId") Integer memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM project_members WHERE member_id = :memberId", nativeQuery = true)
    int deleteProjectMembershipsByMemberId(@Param("memberId") Integer memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM members WHERE id = :memberId", nativeQuery = true)
    int deleteMemberByIdNative(@Param("memberId") Integer memberId);
}
