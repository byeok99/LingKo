package com.lingko.lingko.core.domain.auth.service;

public interface OAuthIdentityVerifier {
    OAuthIdentity verify(String idToken);
}
