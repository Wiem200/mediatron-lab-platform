package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.MemberPublication;
import com.Mediatron.Lab_Management.data_models.MemberPublicationId;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberPublicationRepository extends JpaRepository<MemberPublication, MemberPublicationId> {

    @Query("""
            select mp
            from MemberPublication mp
            join fetch mp.publication p
            where mp.member.id = :memberId
            order by
                case when p.publicationYear is null then 1 else 0 end,
                p.publicationYear desc,
                lower(p.title) asc
            """)
    List<MemberPublication> findAllForMember(@Param("memberId") Integer memberId);

    @Query("""
            select mp
            from MemberPublication mp
            join fetch mp.publication p
            where mp.member.id = :memberId and mp.status = :status
            order by
                case when p.publicationYear is null then 1 else 0 end,
                p.publicationYear desc,
                lower(p.title) asc
            """)
    List<MemberPublication> findAllForMemberByStatus(
            @Param("memberId") Integer memberId,
            @Param("status") PublicationStatus status
    );

    @Query("""
            select mp
            from MemberPublication mp
            join fetch mp.publication p
            where mp.member.id = :memberId and p.id = :publicationId
            """)
    Optional<MemberPublication> findForMemberAndPublication(
            @Param("memberId") Integer memberId,
            @Param("publicationId") Integer publicationId
    );

    @Query("select mp.status from MemberPublication mp where mp.publication.id = :publicationId")
    List<PublicationStatus> findStatusesForPublication(@Param("publicationId") Integer publicationId);

    @Query("""
            select mp.publication.id
            from MemberPublication mp
            where mp.member.id = :memberId
            """)
    List<Integer> findPublicationIdsForMember(@Param("memberId") Integer memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from MemberPublication mp
            where mp.member.id = :memberId
            """)
    int deleteByMemberId(@Param("memberId") Integer memberId);
}