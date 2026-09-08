package com.condos.board.repository;

import com.condos.board.model.Amenity;
import com.condos.board.model.AmenityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AmenityRepository extends MongoRepository<Amenity, String> {

    List<Amenity> findByBoardId(String boardId);

    List<Amenity> findByBoardIdAndStatus(String boardId, AmenityStatus status);
}
