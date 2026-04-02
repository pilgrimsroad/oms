-- PostgreSQL 테스트 데이터 생성 예시
-- 직접 PostgreSQL에서 실행하세요 (Spring 자동 실행 비활성화 상태)

-- ========================
-- 사용자 데이터
-- ========================
INSERT INTO OMS_USER (user_id, user_url, user_type, user_password, use_yn, insert_id, insert_dts)
VALUES
  ('DEMO_USER',   'https://example.com',   '1', 'password',  'Y', 'portfolio', CURRENT_TIMESTAMP),
  ('WEB_USER_01', 'https://web.oms.local', '2', 'password1', 'Y', 'portfolio', CURRENT_TIMESTAMP),
  ('WEB_USER_02', 'https://web.oms.local', '2', 'password2', 'Y', 'portfolio', CURRENT_TIMESTAMP);

-- ========================
-- 메시지 테스트 데이터 50,000건
-- 날짜 범위: 2023-01-01 ~ 2026-03-31
-- msg_type: 1=SMS(비중높음), 2=LMS, 3=MMS, 6=알림톡(비중높음), 7=친구톡, 8=AI알림톡, 9~13=RCS
-- status: 2=전송완료(70%), 4=전송중(10%), 6=전송중(10%), 9=실패(10%)
-- ========================
INSERT INTO TEST_SUBMIT_LOG (subject, message, msg_type, status, submit_time, callback_num, rcpt_data, result, result_desc)
SELECT
    -- 제목
    CASE (i % 15)
        WHEN 0  THEN '[테스트통운] 배송 출발 안내'
        WHEN 1  THEN '[테스트카드] 결제 승인 안내'
        WHEN 2  THEN '[테스트통신] 본인인증 번호 안내'
        WHEN 3  THEN '[테스트마트] 정기 점검 안내'
        WHEN 4  THEN '[테스트은행] 청구서 발송 안내'
        WHEN 5  THEN '[테스트마트] 이달의 프로모션'
        WHEN 6  THEN '[테스트마트] 주문 접수 완료'
        WHEN 7  THEN '[테스트통운] 배송 완료 안내'
        WHEN 8  THEN '[테스트마트] 쿠폰 발급 안내'
        WHEN 9  THEN '[테스트마트] 맞춤 상품 추천'
        WHEN 10 THEN '[테스트통운] RCS 배송 출발 안내'
        WHEN 11 THEN '[테스트은행] RCS 입출금 내역 안내'
        WHEN 12 THEN '[테스트마트] RCS 프로모션 안내'
        WHEN 13 THEN '[테스트보험] RCS 납입 안내'
        ELSE         '[테스트항공] RCS 탑승권 안내'
    END,
    -- 본문
    CASE (i % 15)
        WHEN 0  THEN '[테스트통운] 고객님의 상품(주문번호 TN2025-' || LPAD(i::TEXT, 6, '0') || ')이 출발했습니다. 배송조회: tst.kr/d/' || i
        WHEN 1  THEN '[테스트카드] ' || ((50 + (i % 950)) * 1000) || '원이 승인되었습니다. 일시불. 문의: 1588-0000'
        WHEN 2  THEN '[테스트통신] 인증번호 [' || LPAD((i % 1000000)::TEXT, 6, '0') || ']를 입력해주세요. 타인 노출 금지.'
        WHEN 3  THEN '[테스트마트] 시스템 정기 점검 안내. 일시: ' || (2023 + (i % 3)) || '-' || LPAD((1 + (i % 12))::TEXT, 2, '0') || '-' || LPAD((1 + (i % 28))::TEXT, 2, '0') || ' 02:00~04:00. 서비스가 일시 중단될 수 있습니다.'
        WHEN 4  THEN '[테스트은행] ' || (2023 + (i % 3)) || '년 ' || (1 + (i % 12)) || '월 청구서 발송. 청구금액: ' || ((1 + (i % 100)) * 10000) || '원. 납부기한: 매월 15일'
        WHEN 5  THEN '[테스트마트] 이달의 특가! 최대 ' || (10 + (i % 60)) || '% 할인. 자세히 보기: tst.kr/p/' || i
        WHEN 6  THEN '[테스트마트] 주문번호 TM' || LPAD(i::TEXT, 8, '0') || ' 접수 완료. 상품명: 테스트상품 외 ' || (i % 5) || '건. 결제금액: ' || ((1 + (i % 100)) * 5000) || '원'
        WHEN 7  THEN '[테스트통운] 배송완료. 주문번호: TN' || LPAD(i::TEXT, 8, '0') || '. 문 앞에 배송되었습니다.'
        WHEN 8  THEN '[테스트마트] ' || ((1 + (i % 10)) * 5000) || '원 할인쿠폰 발급. 유효기간: 발급일로부터 30일. tst.kr/c/' || i
        WHEN 9  THEN '[테스트마트] 고객님의 구매 패턴을 분석했습니다. 맞춤 추천 상품을 확인해 보세요. tst.kr/ai/' || i
        WHEN 10 THEN '[테스트통운] 주문번호 TN' || LPAD(i::TEXT, 8, '0') || ' 출발. 예상도착: ' || (2023 + (i % 3)) || '-' || LPAD((1 + (i % 12))::TEXT, 2, '0') || '-' || LPAD((1 + (i % 28))::TEXT, 2, '0')
        WHEN 11 THEN '[테스트은행] 입금 ' || ((1 + (i % 1000)) * 1000) || '원 / 출금 ' || ((1 + (i % 500)) * 1000) || '원 / 잔액 ' || (i % 10000000) || '원'
        WHEN 12 THEN '[테스트마트] ' || (2023 + (i % 3)) || '년 ' || (1 + (i % 12)) || '월 전품목 ' || (10 + (i % 40)) || '% 할인 진행 중!'
        WHEN 13 THEN '[테스트보험] ' || (2023 + (i % 3)) || '년 ' || (1 + (i % 12)) || '월 보험료 ' || ((3 + (i % 20)) * 10000) || '원 자동이체 예정. 이체일: ' || (1 + (i % 28)) || '일'
        ELSE         '[테스트항공] TS' || (100 + (i % 900)) || '편 탑승권. 좌석: ' || (1 + (i % 30)) || chr(65 + (i % 6)) || ' / 탑승구: ' || (1 + (i % 20)) || 'B'
    END,
    -- msg_type (SMS/알림톡 비중 높게)
    CASE (i % 15)
        WHEN 0  THEN 1
        WHEN 1  THEN 1
        WHEN 2  THEN 1
        WHEN 3  THEN 2
        WHEN 4  THEN 2
        WHEN 5  THEN 3
        WHEN 6  THEN 6
        WHEN 7  THEN 6
        WHEN 8  THEN 7
        WHEN 9  THEN 8
        WHEN 10 THEN 9
        WHEN 11 THEN 10
        WHEN 12 THEN 11
        WHEN 13 THEN 12
        ELSE         13
    END,
    -- status (완료 70%, 전송중 20%, 실패 10%)
    CASE (i % 10)
        WHEN 0 THEN 9
        WHEN 1 THEN 4
        WHEN 2 THEN 6
        ELSE        2
    END,
    -- submit_time (2023-01-01 ~ 2026-03-31 분산)
    TO_CHAR(
        '2023-01-01 00:00:00'::TIMESTAMP + (((i::BIGINT * 34157) % (1186 * 24 * 60))::TEXT || ' minutes')::INTERVAL,
        'YYYYMMDDHH24MISS'
    ),
    -- callback_num (발신번호)
    CASE (i % 5)
        WHEN 0 THEN '0215880000'
        WHEN 1 THEN '0215881234'
        WHEN 2 THEN '0215889999'
        WHEN 3 THEN '07012340000'
        ELSE        '07099998888'
    END,
    -- rcpt_data (수신번호)
    '01' || CASE (i % 3) WHEN 0 THEN '0' WHEN 1 THEN '1' ELSE '6' END
         || LPAD(((1000 + (i * 31) % 9000))::TEXT, 4, '0')
         || LPAD(((1000 + (i * 73) % 9000))::TEXT, 4, '0'),
    -- result
    CASE (i % 10)
        WHEN 0 THEN 'FAIL'
        WHEN 1 THEN 'PENDING'
        WHEN 2 THEN 'PENDING'
        ELSE        'SUCCESS'
    END,
    -- result_desc
    CASE (i % 10)
        WHEN 0 THEN CASE (i % 4)
            WHEN 0 THEN 'Temporary error'
            WHEN 1 THEN 'Recipient unreachable'
            WHEN 2 THEN 'Network timeout'
            ELSE        'Invalid number'
        END
        WHEN 1 THEN 'Processing'
        WHEN 2 THEN 'Processing'
        ELSE        'Delivered'
    END
FROM generate_series(1, 50000) AS i;
