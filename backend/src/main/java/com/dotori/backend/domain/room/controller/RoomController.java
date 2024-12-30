package com.dotori.backend.domain.room.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.dotori.backend.domain.room.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dotori.backend.domain.book.model.dto.BookDetailDto;
import com.dotori.backend.domain.book.service.BookService;
import com.dotori.backend.domain.book.service.SceneService;
import com.dotori.backend.domain.room.model.entity.Room;
import com.dotori.backend.domain.room.service.RoomService;

import io.openvidu.java.client.OpenVidu;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@PropertySource("classpath:application-openvidu.yml")
@ConfigurationProperties(prefix = "openvidu")
public class RoomController {

	private OpenVidu openvidu;
	@Value("${url}")
	private String OPENVIDU_URL;
	@Value("${secret}")
	private String OPENVIDU_SECRET;

	private final RoomService roomService;
	private final BookService bookService;
	private final SceneService sceneService;

	@PostConstruct
	public void init() {
		this.openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
	}


	@PostMapping("/session")
	public ResponseEntity<RoomCreationResponseDto> create(
		@RequestBody(required = false) RoomCreationRequestDto requestDto) {
		RoomCreationResponseDto result = roomService.createRoom(openvidu, requestDto);
		return ResponseEntity.ok(result);
	}

	// 삭제는 PathVariable로 하면 안되지..
	@DeleteMapping("/remove-member")
	public ResponseEntity<RoomRemovalResponseDto> removeRoomMember(RoomRemovalRequestDto requestDto) {
		RoomRemovalResponseDto result = roomService.removeMemberFromRoom(openvidu, requestDto.roomId, requestDto.memberId);
		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/remove/expired-room")
	public ResponseEntity<?> removeExpiredRoom() {
		roomService.removeExpiredRooms(openvidu);
		return ResponseEntity.ok().build();
	}

	@GetMapping
	public ResponseEntity<List<RoomResponseDto>> findAllRooms() {

		List<Room> rooms = roomService.getAllRooms();

		List<RoomResponseDto> result = rooms.stream()
				.map(room -> new RoomResponseDto(room))
				.collect(Collectors.toList());

		return ResponseEntity.ok(result);
	}

	@PostMapping("/join-member")
	public ResponseEntity<RoomMemberJoinResponseDto> joinRoomMember(RoomMemberJoinRequestDto requestDto) {

		roomService.joinMemberToRoom(openvidu, requestDto.roomId, requestDto.memberId, requestDto.bookId);

		BookDetailDto bookInfo = BookDetailDto.builder()
				.book(bookService.getBook(requestDto.bookId))
				.roles(bookService.getRolesByBookId(requestDto.bookId))
				.scenes(sceneService.getSceneDetailsByBookId(requestDto.bookId))
				.build();

		RoomMemberJoinResponseDto result = new RoomMemberJoinResponseDto(requestDto.memberId, bookInfo);

		return ResponseEntity.ok(result);
	}

	@PatchMapping("/update/{roomId}")
	public ResponseEntity<Map<String, String>> updateRoom(@PathVariable("roomId") Long roomId,
		@RequestBody(required = true) RoomResponseDto roomInfo) {
		Map<String, String> resultData = new HashMap<>();
		try {
			openvidu.fetch();
			roomService.updateRoom(roomId, roomInfo);
			resultData.put("roomId", String.valueOf(roomId));
			return ResponseEntity.ok(resultData);
		} catch (Exception e) {
			resultData.put("message", e.getMessage());
			return new ResponseEntity<>(resultData, HttpStatus.INTERNAL_SERVER_ERROR);

		}

	}

	@GetMapping("/{roomId}")
	public ResponseEntity<RoomResponseDto> getRoom(@PathVariable("roomId") Long roomId) {
		RoomResponseDto roomInfo = new RoomResponseDto(roomService.getRoom(roomId));
		return ResponseEntity.ok(roomInfo);
	}

}
