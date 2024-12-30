package com.dotori.backend.domain.room.model.dto;

import java.util.Map;

import com.dotori.backend.domain.book.model.dto.BookDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreationRequestDto {
	private Map<String, Object> sessionProperties;
	private Map<String, Object> connectionProperties;
	private Long bookId;
	private Long hostId;
	private String title;
	private String password;
	private boolean isPublic;
	private int limitCnt;
}
