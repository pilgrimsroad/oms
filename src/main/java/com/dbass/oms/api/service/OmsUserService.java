package com.dbass.oms.api.service;

import com.dbass.oms.api.dto.OmsUser;

public interface OmsUserService {
    /**
     * OMS 사용자 등록
     *
     * @param userId 등록요청 사용자 ID
     * @param userUrl 등록요청 사용자 URL
     * @param userType 사용자 타입
     * @param userPassword 사용자 비밀번호
     * @param insertId 등록ID
     * @return 등록된 사용자 정보
     * @throws RuntimeException 이미 등록된 사용자인 경우
     */
    OmsUser registerService(String userId, String userUrl, String userType, String userPassword, String insertId);

    /**
     * JWT 토큰을 발급합니다.
     *
     * @param userId 사용자 ID
     * @param userUrl 사용자 URL
     * @param userPassword 사용자 비밀번호
     * @return 발급된 JWT 토큰
     * @throws RuntimeException 인증 실패 또는 비활성화된 경우
     */
    String issueToken(String userId, String userUrl, String userPassword);

    OmsUser issueTokenUser(String userId, String userUrl, String userPassword);

    /**
     * 사용자를 비활성화합니다.
     *
     * @param userId 사용자 ID
     * @param userUrl 사용자 URL
     * @throws RuntimeException 사용자가 존재하지 않는 경우
     */
    void deactivateUser(String userId, String userUrl);

    /**
     * 웹 사용자 로그인 (userId + password만으로 인증)
     *
     * @param userId 사용자 ID
     * @param userPassword 사용자 비밀번호
     * @return 로그인한 사용자 정보 (JWT 발급용)
     * @throws RuntimeException 인증 실패 또는 비활성화된 경우
     */
    OmsUser loginWeb(String userId, String userPassword);
}
