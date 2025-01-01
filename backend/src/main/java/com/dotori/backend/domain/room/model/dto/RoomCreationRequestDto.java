package com.dotori.backend.domain.room.model.dto;

import java.util.Map;

import com.dotori.backend.domain.book.model.dto.BookDto;

import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@RequiredArgsConstructor
public class RoomCreationRequestDto {
	public final Map<String, Object> sessionProperties;
	public final Map<String, Object> connectionProperties;

	@NotNull
	public final Long bookId;

	@NotNull
	public final Long hostId;

	@NotNull
	public final String title;

	public final String password;

	@NotNull
	public final boolean isPublic;

	@NotNull
	public final int limitCnt;
}
