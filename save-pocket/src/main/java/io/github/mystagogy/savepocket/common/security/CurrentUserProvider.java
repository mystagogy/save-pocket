package io.github.mystagogy.savepocket.common.security;

import io.github.mystagogy.savepocket.common.exception.ErrorCode;
import io.github.mystagogy.savepocket.common.exception.SavePocketException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new SavePocketException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof String principalText) {
            if ("anonymousUser".equals(principalText)) {
                throw new SavePocketException(ErrorCode.UNAUTHORIZED);
            }
            try {
                return Long.parseLong(principalText);
            } catch (NumberFormatException ignored) {
                throw new SavePocketException(ErrorCode.UNAUTHORIZED);
            }
        }

        throw new SavePocketException(ErrorCode.UNAUTHORIZED);
    }
}
