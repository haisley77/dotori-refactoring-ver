package com.dotori.backend.domain.room.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import com.dotori.backend.common.exception.BusinessException;
import com.dotori.backend.common.exception.ErrorCode;
import com.dotori.backend.domain.room.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.dotori.backend.domain.book.model.entity.Book;
import com.dotori.backend.domain.book.repository.BookRepository;
import com.dotori.backend.domain.member.model.entity.Member;
import com.dotori.backend.domain.member.repository.MemberRepository;
import com.dotori.backend.domain.room.model.entity.Room;
import com.dotori.backend.domain.room.model.entity.RoomMember;
import com.dotori.backend.domain.room.repository.RoomMemberRepository;
import com.dotori.backend.domain.room.repository.RoomRepository;

import io.openvidu.java.client.Connection;
import io.openvidu.java.client.ConnectionProperties;
import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.Session;
import io.openvidu.java.client.SessionProperties;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Override
    public RoomCreationResponseDto createRoom(OpenVidu openvidu, RoomCreationRequestDto params) {

        // 책을 조회한다.
        Book book = bookRepository.findById(params.getBookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // openvidu를 최신화한다.
        refreshOpenvidu(openvidu);

        Session session = null;
        Connection connection = null;
        try {
            // 세션을 생성한다.
            if (params.getSessionProperties() == null) {
                session = openvidu.createSession();
            }
            if (params.getSessionProperties() != null) {
                session = openvidu.createSession(SessionProperties.fromJson(params.getSessionProperties()).build());
            }

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new BusinessException(ErrorCode.OPENVIDU_SESSION_NOT_CREATED);
        }

        try {
            // 커넥션을 생성한다.
            if (params.getConnectionProperties() == null) {
                connection = session.createConnection();
            }
            if (params.getConnectionProperties() != null) {
                connection = session.createConnection(ConnectionProperties.fromJson(params.getConnectionProperties()).build());
            }

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new BusinessException(ErrorCode.OPENVIDU_CONNECTION_NOT_CREATED);
        }

        // 대기방을 생성한다.
        Room room = Room.create(params,book, session.getSessionId());
        Room savedRoom = roomRepository.save(room);

        // roomId와 token을 반환한다.
        RoomCreationResponseDto roomCreationResponseDto = new RoomCreationResponseDto(savedRoom.getRoomId(),connection.getToken());

        return roomCreationResponseDto;
    }

    private Session findSessionByRoomId(OpenVidu openvidu, Long roomId) {

        // 방을 조회한다.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        return openvidu.getActiveSession(room.getSessionId());
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByIsRecordingAscCreatedAtDesc();
    }

    @Override
    public String createConnection(OpenVidu openvidu, RoomConnectionRequestDto requestDto) {

        // 방을 조회한다.
        Room room = roomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 참여 가능 방인지 확인한다.
        checkJoinPossible(openvidu, room);

        refreshOpenvidu(openvidu);

        // 세션을 조회한다.
        Session session = findSessionByRoomId(openvidu, requestDto.getRoomId());

        Connection connection = null;

        try {
            // 커넥션을 생성한다.
            if (requestDto.getConnectionProperties() == null) {
                connection = session.createConnection();
            }
            if (requestDto.getConnectionProperties() != null) {
                connection = session.createConnection(ConnectionProperties.fromJson(requestDto.getConnectionProperties()).build());
            }
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new BusinessException(ErrorCode.OPENVIDU_CONNECTION_NOT_CREATED);
        }

        return connection.getToken();
    }

    private void checkJoinPossible(OpenVidu openvidu, Room room) {

        // 방에 연결된 유효한 connection 리스트를 openvidu 서버에서 불러온다.
        List<Connection> activeConnections = openvidu.getActiveSession(room.getSessionId()).getActiveConnections();

        if (activeConnections.size() >= room.getLimitCnt()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

        if (room.getJoinCnt() >= room.getLimitCnt()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);
        }

    }

    @Override
    public Member joinMemberToRoom(OpenVidu openvidu, Long roomId, Long memberId, Long bookId) {

        // 방을 조회한다.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 참여 가능 방인지 확인한다.
        checkJoinPossible(openvidu, room);

        refreshOpenvidu(openvidu);

        // 멤버를 방에 추가한다.
        Member member = memberRepository.findById(memberId).get();
        RoomMember roomMember = new RoomMember(member, room);
        roomMemberRepository.save(roomMember);

        // 참여 인원을 갱신한다.
        Room.updateJoinCnt(room, room.getJoinCnt() + 1);

        return member;
    }

    @Override
    public Long removeMemberFromRoom(OpenVidu openvidu, Long roomId, Long memberId) {

        refreshOpenvidu(openvidu);

        // 방 참여 멤버를 DB에서 지운다.
        RoomMember roomMember = roomMemberRepository.findByRoomRoomIdAndMemberMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND)
        );
        roomMemberRepository.delete(roomMember);

        // 방을 조회한다.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 방 인원을 갱신한다.
        Room.updateJoinCnt(room, room.getJoinCnt() - 1);

        // 방에 남은 인원이 없으면 제거한다.
        if (room.getJoinCnt() == 0) {
            roomRepository.delete(room);
        }

        return memberId;
    }

    @Override
    public Room updateRoom(OpenVidu openvidu, Long roomId) {

        refreshOpenvidu(openvidu);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        Room.updateStatus(room);
        return room;
    }

    @Override
    public void removeExpiredRooms(OpenVidu openvidu) {

        refreshOpenvidu(openvidu);

        List<Session> activeSessions = openvidu.getActiveSessions();

        List<String> activeSessionIdList = activeSessions.stream()
                .map(Session::getSessionId)
                .collect(Collectors.toList());

        roomRepository.deleteAllBySessionIdNotIn(activeSessionIdList);
    }

    private void refreshOpenvidu(OpenVidu openvidu) {
        try {
            openvidu.fetch();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new BusinessException(ErrorCode.OPENVIDU_NOT_FETCHED);
        }
    }

}