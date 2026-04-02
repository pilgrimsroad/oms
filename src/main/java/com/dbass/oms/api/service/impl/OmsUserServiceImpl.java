package com.dbass.oms.api.service.impl;

import com.dbass.oms.api.dto.OmsUser;
import com.dbass.oms.api.repository.OmsUserRepository;
import com.dbass.oms.api.security.JwtTokenProvider;
import com.dbass.oms.api.service.OmsUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OmsUserServiceImpl implements OmsUserService {

    private final OmsUserRepository omsUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public OmsUser registerService(String userId, String userUrl, String userType, String userPassword, String insertId) {
        if (omsUserRepository.existsByUserIdAndUserUrl(userId, userUrl)) {
            throw new RuntimeException("이미 등록된 사용자ID입니다.");
        }

        OmsUser omsUser = new OmsUser();
        omsUser.setUserId(userId);
        omsUser.setUserUrl(userUrl);
        omsUser.setUserType(userType);
        omsUser.setUserPassword(passwordEncoder.encode(userPassword));
        omsUser.setUseYn("Y");
        omsUser.setInsertDts(LocalDateTime.now());
        omsUser.setInsertId(insertId);

        return omsUserRepository.save(omsUser);
    }

    @Override
    @Transactional
    public String issueToken(String userId, String userUrl, String userPassword) {
        OmsUser omsUser = omsUserRepository.findByUserIdAndUserUrl(userId, userUrl)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!"Y".equals(omsUser.getUseYn())) {
            throw new RuntimeException("비활성화된 사용자입니다.");
        }

        if (!passwordEncoder.matches(userPassword, omsUser.getUserPassword())) {
            // Demo fallback: allow legacy plaintext seeds, then upgrade to bcrypt.
            if (userPassword != null && userPassword.equals(omsUser.getUserPassword())) {
                omsUser.setUserPassword(passwordEncoder.encode(userPassword));
                omsUserRepository.save(omsUser);
            } else {
                throw new RuntimeException("사용자 비밀번호가 올바르지 않습니다.");
            }
        }

        return jwtTokenProvider.generateToken(omsUser);
    }

    @Override
    @Transactional
    public OmsUser loginWeb(String userId, String userPassword) {
        OmsUser omsUser = omsUserRepository.findByUserIdAndUserType(userId, "2")
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!"Y".equals(omsUser.getUseYn())) {
            throw new RuntimeException("비활성화된 사용자입니다.");
        }

        if (!passwordEncoder.matches(userPassword, omsUser.getUserPassword())) {
            if (userPassword != null && userPassword.equals(omsUser.getUserPassword())) {
                omsUser.setUserPassword(passwordEncoder.encode(userPassword));
                omsUserRepository.save(omsUser);
            } else {
                throw new RuntimeException("사용자 비밀번호가 올바르지 않습니다.");
            }
        }

        return omsUser;
    }

    @Override
    @Transactional
    public void deactivateUser(String userId, String userUrl) {
        OmsUser omsUser = omsUserRepository.findByUserIdAndUserUrl(userId, userUrl)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        omsUser.setUseYn("N");
        omsUserRepository.save(omsUser);
    }
}
