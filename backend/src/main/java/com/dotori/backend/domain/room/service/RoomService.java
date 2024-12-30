package com.dotori.backend.domain.room.service;

import java.util.List;
import java.util.Map;

import com.dotori.backend.domain.room.model.dto.*;
import com.dotori.backend.domain.room.model.entity.Room;

import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;

public interface RoomService {

	RoomCreationResponseDto createRoom(OpenVidu openvidu, RoomCreationRequestDto requestDto);

	Session findSessionByRoomId(OpenVidu openvidu, Long roomId) throws Exception;

	List<Room> getAllRooms();

	String createConnection(OpenVidu openvidu, Session session,
		Map<String, Object> connectionProperties) throws
		OpenViduJavaClientException,
		OpenViduHttpException;

	void joinMemberToRoom(OpenVidu openvidu, Long roomId, Long memberId, Long bookId);

	RoomRemovalResponseDto removeMemberFromRoom(OpenVidu openvidu, Long roomId, Long memberId);

	void updateRoom(Long roomId, RoomResponseDto roomInfo);

	Room getRoom(Long roomId);

	void removeExpiredRooms(OpenVidu openvidu);
}
