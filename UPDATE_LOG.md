# 업데이트 내역

---

### 2026-04-14

**발송 대기 현황 조회**
- `GET /api/agent/pending-count` 엔드포인트 추가 — 현재 발송 대기(status=0) 건수 반환
- `MessageRepository`: `countByStatus()` 추가
- `MessageService` / `MessageServiceImpl`: `getPendingCount()` 추가
- 프론트엔드 `AgentPage`: 진입 시 자동으로 대기 건수 조회, 처리 실행 후 건수 갱신
  - 대기 0건 → "현재 발송 대기 건이 없습니다." 안내
  - 대기 N건 → 주황색으로 건수 강조 표시

**Spring Boot Actuator + Spring Boot Admin 도입**
- 기존 자체 HealthCheckController 제거 → Spring Boot Actuator 전환
- 노출 엔드포인트: health, info, metrics, loggers, caches, threaddump, env
- Spring Boot Admin 3.2.3 UI 도입 (`/admin`) — 인스턴스 상태, 메트릭, 로거 실시간 모니터링
- Docker 환경에서 서비스 URL 명시 (`OMS_BASE_URL` 환경변수)

**Refresh Token 도입**
- Access Token: 30분, Refresh Token: 7일 (UUID, Redis 저장)
- `POST /api/auth/refresh`: 토큰 갱신 엔드포인트 신규 추가
- Refresh Token Rotation: 갱신 시마다 기존 토큰 삭제 + 신규 발급
- 로그아웃 시 Refresh Token Redis에서 삭제
- Redis 키 패턴: `refresh:{token}` → `userId|userUrl|userType` (TTL 7일)

**발송 기능 구현**
- `POST /api/messages/send`: TEST_SUBMIT_LOG에 status=0(대기)으로 insert
- `POST /api/agent/process`: 발송 대기 건 일괄 처리 → status=2(완료) 업데이트 (로컬 시뮬레이션용)

**역할 기반 접근 제어 강화**
- user_type 체계 개편: 99(관리자), 1(API), 2(발송가능), 3(일반)
- type=99/1: 전체 API 접근
- type=2: 발송 조회 + 발송 요청
- type=3: 발송 조회만

**프론트엔드 개선 (oms_front)**
- Layout 컴포넌트: 공통 헤더, 역할 기반 네비게이션 메뉴
- 발송 요청 페이지(SendPage): 메시지 유형 선택, 발신/수신 번호, 제목, 내용, 예약 시간 입력
  - SMS/LMS/MMS 바이트 카운터 (한글 2byte, 영문 1byte)
  - SMS=80byte, LMS·MMS=2000byte 초과 시 발송 버튼 비활성화
- 에이전트 처리 페이지(AgentPage): 발송 대기 일괄 처리 버튼
- Axios 인터셉터: 401 응답 시 자동으로 `/api/auth/refresh` 호출 후 재시도
- 브라우저 탭 타이틀 → "OMS"

**JUnit 테스트 추가 (63건 → 82건)**
- `MessageSendControllerTest`: 발송 요청/에이전트 처리 API 10건
- `RefreshTokenControllerTest`: Refresh Token 발급/갱신/검증 9건

---

### 2026-04-13

**테스트 전면 구축 (63건, 0 failures)**
- `MessageRepositoryImplTest`: QueryDSL 동적 쿼리 슬라이스 테스트 15건 (날짜 범위, 단일/복합 조건 필터, 페이징, 정렬)
- `MessageServiceImplTest`: 서비스 계층 순수 단위 테스트 17건 (날짜 포맷 변환, msgType 파싱, 코드명 매핑)
- `AuthControllerTest`: 인증 API 통합 테스트 23건 (등록/토큰발급/웹로그인/로그아웃 성공·실패 케이스)
- `MessageControllerTest`: 메시지 조회 API 통합 테스트 8건 (인증 성공·실패, 입력값 검증)
- `MessageCacheTest`: Redis 캐시 동작 통합 테스트 4건 (캐시 히트, 조건별 별도 키)
- `TestConfig`, `application-test.yml` 신규 추가 — Redis Mock, H2 슬라이스 환경 구성
- `schema-h2.sql` `CREATE TABLE IF NOT EXISTS` 처리, `data-h2.sql` 멱등성(TRUNCATE→INSERT) 적용

**DB 접속 정보 환경변수화**
- `application.yml` DB `username`, `password` 하드코딩 → `${DB_USERNAME}`, `${DB_PASSWORD}` 환경변수 처리
- CORS allowed-origins `${CORS_ALLOWED_ORIGINS}` 환경변수 처리

**schema-postgresql.sql 인덱스 추가**
- `TEST_SUBMIT_LOG`: `submit_time`, `status`, `msg_type` 인덱스 추가
- `OMS_USER`: `user_id`, `user_url`, `user_type` 복합 인덱스 추가

**페이징 기능 개선**
- 전체 조회 후 페이지에서 구분 → 지정 단위별 페이지 조회로 변경 (서버 부하 감소)

**QueryDSL 설정 개선**
- Q클래스 생성 경로 `build/generated/querydsl` → `src/main/generated` 변경 (IDE 인식 개선)
- `MessageRepositoryImpl` `fetchOne()` 반환값 null 처리 추가 (NPE 방지)

**Redis 역직렬화 오류 수정**
- `PagedResponseDto` `@NoArgsConstructor` 추가 (Jackson 역직렬화 오류 해결)

---

### 2026-04-02

**Redis 도입 (캐싱 / JWT 블랙리스트)**
- `spring-boot-starter-data-redis` 의존성 추가
- JWT 블랙리스트: 로그아웃 시 토큰을 Redis에 저장(TTL = 남은 만료시간), 이후 동일 토큰 요청 차단
- 메시지 조회 캐싱: 동일 검색 조건 반복 조회 시 Redis에서 응답 (TTL 5분)
- `TokenBlacklistService`, `RedisConfig`, `CacheConfig` 클래스 신규 추가
- Redis Insight 관제 도구를 Docker Compose에 포함 (포트 8001)

**DB 전환 (H2 → PostgreSQL)**
- `application.yml`에 PostgreSQL / H2 블록 분리 (주석 전환 방식으로 관리)
- `schema-postgresql.sql`, `data-postgresql.sql` 신규 추가
- `OmsUser.insertDts`, `updateDts` 타입 `String` → `LocalDateTime` 변경
- JWT claim 내 `userType` 공백 패딩 처리 (`CHAR(2)` → trim)

**프론트엔드 개선 (oms_front)**
- 날짜 선택 컴포넌트를 커스텀 DatePicker → Ant Design RangePicker로 교체
- 조회 결과 100건 단위 페이지네이션 추가
- 총 건수 3자리 콤마 포맷 적용
