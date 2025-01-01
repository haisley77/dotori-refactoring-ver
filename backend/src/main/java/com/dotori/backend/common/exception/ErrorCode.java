package com.dotori.backend.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Buisiness
    NOT_FOUND("B-001", "Entity Not Found"),

    // Member
    MEMBER_NOT_FOUND("M-001", "Member Not Found"),

    // Room
    ROOM_NOT_FOUND("R-001", "Room Not Found"),
    ROOM_NOT_AVAILABLE("R-002", "Room Not Available"),
    ROOM_MEMBER_NOT_FOUND("R-003", "Room Member Not Found"),

    // Book
    BOOK_NOT_FOUND("B-001", "Book Not Found"),
    SCENE_NOT_FOUND("B-001", "Scene Not Found"),

    // Openvidu
    OPENVIDU_CONNECTION_NOT_CREATED("O-001", "Openvidu Connection Not Created"),
    OPENVIDU_SESSION_NOT_CREATED("O-002", "Openvidu Session Not Created"),
    OPENVIDU_NOT_FETCHED("O-003", "Openvidu Not Fetched"),

    // Video
    CHUNK_FILES_NOT_UPLOADED("V-001", "Chunk Files Not Upload"),
    CHUNK_FILES_NOT_MERGED("V-002", "Chunk Files Not Merged"),
    VIDEO_NOT_MERGED("V-003", "Video Not Merged"),
    VIDEO_NOT_FOUND("V-004", "Video Not Found");

    private final String code;
    private final String message;

    ErrorCode(final String code, final String message) {
        this.code = code;
        this.message = message;
    }
}
