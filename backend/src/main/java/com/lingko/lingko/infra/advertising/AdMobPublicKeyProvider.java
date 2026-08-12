package com.lingko.lingko.infra.advertising;

import java.security.PublicKey;

/** Google key_id를 현재 신뢰 가능한 AdMob ECDSA 공개키로 해석한다. */
@FunctionalInterface
public interface AdMobPublicKeyProvider {
    PublicKey getKey(long keyId);
}
