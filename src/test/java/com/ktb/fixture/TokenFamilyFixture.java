package com.ktb.fixture;

import com.ktb.auth.dto.TokenFamilyInfo;
import java.util.Optional;

public class TokenFamilyFixture {

    public static final Long DEFAULT_FAMILY_ID = 10L;
    public static final String DEFAULT_FAMILY_UUID = "family-uuid-123";

    public static TokenFamilyInfo activeFamily() {
        return new TokenFamilyInfo(DEFAULT_FAMILY_ID, DEFAULT_FAMILY_UUID, true);
    }

    public static TokenFamilyInfo revokedFamily() {
        return new TokenFamilyInfo(DEFAULT_FAMILY_ID, DEFAULT_FAMILY_UUID, false);
    }

    public static Optional<TokenFamilyInfo> activeFamilyOptional() {
        return Optional.of(activeFamily());
    }

    public static Optional<TokenFamilyInfo> revokedFamilyOptional() {
        return Optional.of(revokedFamily());
    }
}
