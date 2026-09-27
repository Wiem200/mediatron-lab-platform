package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.SubscriberContentNotification;
import com.Mediatron.Lab_Management.data_models.SubscriberContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriberContentNotificationRepository extends JpaRepository<SubscriberContentNotification, Long> {

    boolean existsBySubscriber_IdAndContentTypeAndContentId(
            Integer subscriberId,
            SubscriberContentType contentType,
            Integer contentId
    );
}