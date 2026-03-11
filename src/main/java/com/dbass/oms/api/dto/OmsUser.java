package com.dbass.oms.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * OMS 사용자 DTO
 */
@Data
@Entity
@Table(name = "OMS_USER")
public class OmsUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    @Schema(description = "사용자 시퀀스 ID")
    private Long userSeq;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "사용자 ID")
    private String userId;

    @Column(name = "user_url", nullable = false)
    @Schema(description = "사용자 URL")
    private String userUrl;

    @Column(name = "user_type")
    @Schema(description = "사용자 타입")
    private String userType;

    @JsonIgnore
    @Column(name = "user_password", nullable = false)
    @Schema(description = "비밀번호(저장용)", hidden = true)
    private String userPassword;

    @Column(name = "use_yn")
    @Schema(description = "사용 여부 (Y/N)")
    private String useYn;

    @Column(name = "insert_dts")
    @Schema(description = "등록일")
    private String insertDts;

    @Column(name = "insert_id")
    @Schema(description = "등록자 ID")
    private String insertId;

    @Column(name = "update_dts")
    @Schema(description = "수정일")
    private String updateDts;

    @Column(name = "update_id")
    @Schema(description = "수정자 ID")
    private String updateId;
}
