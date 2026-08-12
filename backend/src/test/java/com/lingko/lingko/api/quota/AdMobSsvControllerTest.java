package com.lingko.lingko.api.quota;

import com.lingko.lingko.core.domain.quota.service.AdRewardService;
import com.lingko.lingko.core.domain.quota.service.VerifiedAdRewardCallback;
import com.lingko.lingko.infra.advertising.AdMobSsvVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 공개 SSV endpoint가 raw query 검증 뒤에만 보상 서비스를 호출하는지 검증한다. */
@WebMvcTest(AdMobSsvController.class)
class AdMobSsvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdMobSsvVerifier verifier;

    @MockitoBean
    private AdRewardService adRewardService;

    @Test
    @DisplayName("Google signed callback의 raw query를 검증하고 200을 반환한다")
    void acceptsVerifiedCallback() throws Exception {
        String query = "ad_unit=3927267131&signature=signed&key_id=1";
        VerifiedAdRewardCallback callback = new VerifiedAdRewardCallback(
                "3927267131", "token", 1, "pronunciation_chance", 1L, "transaction"
        );
        when(verifier.verify(query)).thenReturn(callback);

        mockMvc.perform(get("/api/quota/ad-rewards/ssv?" + query))
                .andExpect(status().isOk());

        verify(adRewardService).processVerifiedCallback(callback);
    }
}
