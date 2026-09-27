package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.MemberPublicationNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberPublicationNotificationRepository extends JpaRepository<MemberPublicationNotification, Long> {

    boolean existsByMember_IdAndPublication_IdAndNotificationType(
            Integer memberId,
            Integer publicationId,
            String notificationType
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from MemberPublicationNotification notification
            where notification.member.id = :memberId
            """)
    int deleteByMemberId(@Param("memberId") Integer memberId);
}