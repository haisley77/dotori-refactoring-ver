package com.dotori.backend.domain.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@RequiredArgsConstructor
public class RoomRemovalRequestDto {
    @NotNull
    public final Long roomId;

    @NotNull
    public final Long memberId;
}
