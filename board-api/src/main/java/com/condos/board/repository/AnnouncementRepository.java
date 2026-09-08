package com.condos.board.repository;

import com.condos.board.model.Announcement;
import com.condos.board.model.AnnouncementStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {

    List<Announcement> findByBoardIdOrderByCreatedAtDesc(String boardId);

    List<Announcement> findByBoardIdAndStatusOrderByCreatedAtDesc(String boardId, AnnouncementStatus status);
}
