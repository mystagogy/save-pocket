package io.github.mystagogy.savepocket.auth.dto;

public record AuthUserResponse(
        Long id,
        String email,
        String nickname
) {
}
