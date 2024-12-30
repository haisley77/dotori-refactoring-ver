package com.dotori.backend.domain.room.model.dto;

import com.dotori.backend.domain.book.model.dto.BookDetailDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class RoomMemberJoinResponseDto {
    private Long memberId;
    private BookDetailDto bookInfo;
}
