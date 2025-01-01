package com.dotori.backend.domain.room.model.dto;

import com.dotori.backend.domain.book.model.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class RoomUpdateRequestDto {
    private final Long roomId;
}
