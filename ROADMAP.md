# 개발 로드맵

앞으로 진행할 기능 목록입니다. 우선순위 순으로 정렬되어 있습니다.

---

## 1단계 — 계정 관리

### 1-1. 비밀번호 초기화 (관리자)

**흐름:**
1. 관리자가 특정 계정의 비밀번호를 초기화 → 임시 비밀번호 직접 지정
2. DB에 임시 비밀번호(BCrypt 해시) 저장 + `password_reset_required = true` 플래그 설정
3. 해당 계정으로 로그인 시 서버가 플래그 감지 → 로그인 응답에 `passwordResetRequired: true` 포함
4. 프론트엔드에서 감지 → 비밀번호 변경 페이지로 강제 이동
5. 새 비밀번호 입력 후 저장 → 플래그 해제 → 정상 사용

**변경 범위:**
- `OMS_USER` 테이블에 `password_reset_required` 컬럼 추가
- `POST /api/auth/admin/reset-password` 엔드포인트 추가 (type=99 전용)
- 로그인 응답 DTO에 `passwordResetRequired` 필드 추가
- `POST /api/auth/change-password` 엔드포인트 추가
- 프론트엔드: 비밀번호 변경 페이지(ChangePasswordPage) 추가, 강제 이동 처리

---

### 1-2. 계정 관리 페이지 (관리자)

**흐름:**
- 관리자 전용 페이지에서 전체 계정 목록 조회
- 계정별 활성/비활성 토글, 비밀번호 초기화 버튼 제공
- 신규 계정 등록 폼 (현재 `/api/auth/register` API 직접 호출 방식을 UI로 제공)

**변경 범위:**
- `GET /api/auth/users` 엔드포인트 추가 (type=99 전용)
- `PATCH /api/auth/users/{userId}/status` 엔드포인트 추가
- 프론트엔드: 계정 관리 페이지(UserManagePage) 추가

---

## 2단계 — 메시지 기능 보강

### 2-1. 조회 기간 빠른 선택 버튼

**흐름:**
- 조회 조건 날짜 입력 아래에 버튼 그룹 추가
- 버튼 클릭 시 오늘 기준으로 시작일/종료일 자동 계산 후 RangePicker에 반영
- 버튼 종류: 1주 / 1달 / 3개월 / 6개월 / 1년

**변경 범위:**
- 프론트엔드 `MessagePage`에 버튼 그룹 UI 추가
- 클릭 이벤트에서 날짜 계산 후 기존 RangePicker 상태값 업데이트 (백엔드 변경 없음)

---

### 2-2. 발송 결과 상세 조회

**흐름:**
- MessagePage 목록에서 특정 건 클릭 → 우측 또는 모달로 상세 정보 표시
- 상세 항목: msgId, 메시지 유형, 발신/수신 번호, 제목, 내용, 상태, 요청일시, 처리일시, 결과 코드

**변경 범위:**
- `GET /api/messages/{msgId}` 엔드포인트 추가
- 프론트엔드: 클릭 이벤트 + 상세 모달 컴포넌트 추가

---

### 2-3. 발송 통계

**흐름:**
- 관리자/발송가능 계정에서 기간별 발송 현황 조회
- 상태별 건수 (대기/처리중/완료/실패), 유형별 건수, 일별 추이

**변경 범위:**
- `GET /api/messages/stats` 엔드포인트 추가 (집계 쿼리)
- 프론트엔드: 통계 페이지(StatsPage) 추가, 차트 라이브러리 연동 (Recharts 등)

---

## 3단계 — 안정성 보강

### 3-1. 예외 처리 통합

**흐름:**
- 현재 에러 응답 형식이 일부 불일치 → `@ControllerAdvice`에서 전체 통일
- 공통 에러 응답 형식: `{ "error": "메시지", "code": "ERROR_CODE", "timestamp": "..." }`

**변경 범위:**
- `GlobalExceptionHandler` 개선
- 에러 코드 enum 정의

---

### 3-2. API Rate Limiting

**흐름:**
- 동일 IP 또는 동일 계정에서 단시간 과다 요청 차단
- 초과 시 `429 Too Many Requests` 응답

**변경 범위:**
- Bucket4j 의존성 추가
- `RateLimitFilter` 또는 AOP로 적용
- 엔드포인트별 제한 기준 설정 (예: /api/messages/send → 분당 60건)

---

## 4단계 — 코드 품질

### 4-1. OmsUser DTO 분리

**흐름:**
- 현재 `OmsUser` Entity가 인증 컨텍스트에서 직접 사용됨
- `UserPrincipal` (인증용), `UserResponseDto` (응답용)으로 분리하여 민감 필드 노출 차단

**변경 범위:**
- `UserResponseDto` 클래스 추가 (password 필드 제외)
- `JwtAuthenticationFilter`, 서비스 계층 참조 변경
