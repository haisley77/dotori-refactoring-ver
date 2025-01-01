package com.dotori.backend.domain.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class RoomConnectionRequestDto {
    @NotNull
    public final Long roomId;
    public final Map<String, Object> connectionProperties;
}
