package com.kaveri.repository;

import com.kaveri.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    @Query("SELECT c FROM ContactMessage c WHERE " +
           "(:keyword IS NULL OR LOWER(c.senderName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(c.senderEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(c.ventureDepartment) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(c.senderMessage) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isRead IS NULL OR c.isRead = :isRead)")
    Page<ContactMessage> searchMessages(
        @Param("keyword") String keyword,
        @Param("isRead") Boolean isRead,
        Pageable pageable
    );

    long countByIsReadFalse();
}
