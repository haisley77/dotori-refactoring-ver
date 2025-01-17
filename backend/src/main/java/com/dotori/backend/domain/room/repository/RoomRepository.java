package com.dotori.backend.domain.room.repository;

import java.util.List;
import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;

import com.dotori.backend.domain.room.model.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
	List<Room> findAllByOrderByIsRecordingAscCreatedAtDesc();
	int deleteAllBySessionIdNotIn(List<String> activeSessionIdList);
}
