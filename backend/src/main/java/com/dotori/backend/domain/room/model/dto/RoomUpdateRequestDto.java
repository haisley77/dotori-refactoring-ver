package com.dotori.backend.domain.room.model.dto;

import com.dotori.backend.domain.book.model.entity.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@RequiredArgsConstructor
public class RoomUpdateRequestDto {
    @NotNull
    public final Long roomId;
}
