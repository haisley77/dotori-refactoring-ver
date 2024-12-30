package com.dotori.backend.domain.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRemovalRequestDto {
    public Long roomId;
    public Long memberId;
}
