package com.dotori.backend.domain.room.model.dto;

import java.util.Map;

import com.dotori.backend.domain.book.model.dto.BookDto;

import lombok.*;

@Data
@RequiredArgsConstructor
public class RoomCreationRequestDto {
	private final Map<String, Object> sessionProperties;
	private final Map<String, Object> connectionProperties;
	private final Long bookId;
	private final Long hostId;
	private final String title;
	private final String password;
	private final boolean isPublic;
	private final int limitCnt;
}
