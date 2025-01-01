package com.dotori.backend.domain.room.service;

import java.util.List;
import java.util.Map;

import com.dotori.backend.domain.member.model.entity.Member;
import com.dotori.backend.domain.room.model.dto.*;
import com.dotori.backend.domain.room.model.entity.Room;

import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;

public interface RoomService {

	RoomCreationResponseDto createRoom(OpenVidu openvidu, RoomCreationRequestDto requestDto);
	List<Room> getAllRooms();
	Room getRoom(Long roomId);
	String createConnection(OpenVidu openvidu, RoomConnectionRequestDto requestDto);
	Member joinMemberToRoom(OpenVidu openvidu, Long roomId, Long memberId, Long bookId);
	Long removeMemberFromRoom(OpenVidu openvidu, Long roomId, Long memberId);
	Room updateRoom(OpenVidu openvidu, Long roomId);
	void removeExpiredRooms(OpenVidu openvidu);
}
