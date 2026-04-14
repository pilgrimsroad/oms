package com.dbass.oms.api.repository;

import com.dbass.oms.api.dto.OmsUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OmsUserRepository extends JpaRepository<OmsUser, Long> {
    Optional<OmsUser> findByUserIdAndUserUrl(String userId, String userUrl);
    boolean existsByUserIdAndUserUrl(String userId, String userUrl);
    Optional<OmsUser> findByUserIdAndUserType(String userId, String userType);
    Optional<OmsUser> findByUserIdAndUserTypeIn(String userId, List<String> types);
}
