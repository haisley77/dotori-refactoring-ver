package com.dotori.backend.domain.member.model.dto;

import com.dotori.backend.domain.member.model.entity.Member;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class MemberResponseDto {
    private String nickName;
    private String email;
    private Long memberId;
    private String profileImg;

    public MemberResponseDto(Member member) {
        this.nickName = member.getNickname();
        this.email = member.getEmail();
        this.memberId = member.getMemberId();
        this.profileImg = member.getProfileImg();
    }
}
