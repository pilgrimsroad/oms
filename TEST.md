# 테스트 가이드

OMS API 테스트 전체 현황 및 실행 방법 정리

---

## 전체 현황

| 테스트 클래스 | 유형 | 건수 | 위치 |
|---|---|---|---|
| `MessageRepositoryImplTest` | Repository (QueryDSL 슬라이스) | 15 | `repository/` |
| `MessageServiceImplTest` | Service (순수 단위) | 17 | `service/` |
| `AuthControllerTest` | Controller 통합 (인증 API) | 23 | `controller/` |
| `MessageControllerTest` | Controller 통합 (메시지 조회 API) | 8 | `controller/` |
| `MessageCacheTest` | Cache 동작 통합 | 4 | `service/` |
| **합계** | | **63** | |

모든 테스트는 외부 인프라(PostgreSQL, Redis) 없이 실행됩니다.

---

## 실행 방법

### 전체 테스트

```bash
./gradlew test
```

### 클래스 단위 실행

```bash
./gradlew test --tests "com.dbass.oms.api.repository.MessageRepositoryImplTest"
./gradlew test --tests "com.dbass.oms.api.service.MessageServiceImplTest"
./gradlew test --tests "com.dbass.oms.api.controller.AuthControllerTest"
./gradlew test --tests "com.dbass.oms.api.controller.MessageControllerTest"
./gradlew test --tests "com.dbass.oms.api.service.MessageCacheTest"
```

### 특정 메서드 단위 실행

```bash
# 클래스명.메서드명
./gradlew test --tests "com.dbass.oms.api.repository.MessageRepositoryImplTest.DateRange.inRange"
```

### 결과 리포트 확인

```bash
# 테스트 실행 후 HTML 리포트 열기
open build/reports/tests/test/index.html
```

---

## 테스트 환경 구성

### 핵심 파일

| 파일 | 역할 |
|---|---|
| `src/test/resources/application-test.yml` | 테스트 전용 프로파일 설정 (H2, 슬라이스 전용 SQL 경로 등) |
| `src/test/java/.../config/TestConfig.java` | Redis Bean을 Mock으로 대체하는 `@TestConfiguration` |
| `src/main/resources/sql/ddl/schema-h2.sql` | H2 테이블 DDL (`CREATE TABLE IF NOT EXISTS`) |
| `src/main/resources/sql/ddl/data-h2.sql` | H2 초기 데이터 (TRUNCATE → INSERT, 멱등성 보장) |

### 외부 의존성 처리 방식

| 의존성 | 처리 |
|---|---|
| **Redis** (RedisConnectionFactory, RedisTemplate) | `TestConfig`에서 Mockito Mock으로 대체 |
| **CacheManager** (RedisCacheManager) | 각 통합 테스트에서 `@MockBean`으로 대체, `ConcurrentMapCache` stub 주입 |
| **PasswordEncoder** (BCryptPasswordEncoder) | 각 통합 테스트에서 `@MockBean`으로 대체, plain-text 비교 stub |
| **TokenBlacklistService** | 각 통합 테스트에서 `@MockBean`으로 대체 |
| **PostgreSQL** | `@DataJpaTest`는 H2 인메모리 사용, `@SpringBootTest`는 `application-test.yml`의 H2 URL 사용 |

### H2 멱등성 처리

여러 Spring 컨텍스트(`@SpringBootTest`, `@DataJpaTest`)가 테스트 실행 중 동시에 존재할 수 있으며, 모두 동일한 H2 인메모리 DB(`jdbc:h2:mem:oms`)를 공유합니다.

- `schema-h2.sql`: `CREATE TABLE IF NOT EXISTS`로 중복 실행 안전
- `data-h2.sql`: 파일 상단 `TRUNCATE TABLE` / `DELETE FROM`으로 중복 삽입 방지

---

## 테스트 클래스별 상세

### 1. MessageRepositoryImplTest

**유형**: `@DataJpaTest` (JPA 슬라이스) + `@Import(QueryDslConfig.class)`  
**DB**: H2 인메모리 (`create-drop`)  
**데이터**: `@BeforeEach`에서 `TestEntityManager`로 4건 직접 삽입

```
데이터 셋업
├── (20250101, msgType=1, status=2, rcpt=01011111111)  SMS 성공
├── (20250201, msgType=2, status=9, rcpt=01022222222)  LMS 실패
├── (20250301, msgType=1, status=2, rcpt=01033333333)  SMS 성공
└── (20260101, msgType=1, status=2, rcpt=01044444444)  다른 연도(2026)
```

| 그룹 | 테스트 | 검증 내용 |
|---|---|---|
| 날짜 범위 | 2025년 범위 조회 | totalElements = 3 |
| 날짜 범위 | 2026년 범위 조회 | totalElements = 1 |
| 날짜 범위 | 범위 밖 조회 | totalElements = 0, content 비어있음 |
| 단일 조건 | status=2 필터 | 2건, 전부 status=2 |
| 단일 조건 | status=9 필터 | 1건, status=9 |
| 단일 조건 | msgType=1 필터 | 2건, 전부 msgType=1 |
| 단일 조건 | recipient 부분 일치 | "010222" → 1건 |
| 단일 조건 | recipient 전체 일치 | "01033333333" → 1건 |
| 복합 조건 | msgType=1 + status=2 | 2건, 모두 조건 충족 |
| 복합 조건 | msgType=1 + recipient="010111" | 1건 |
| 복합 조건 | 없는 조건(status=99) | 0건 |
| 페이징 | size=2 | content 2건, totalElements=3, totalPages=2 |
| 페이징 | 2페이지 조회 | content 1건 |
| 정렬 | submit_time 내림차순 | 시간 역순 정렬 검증 |

---

### 2. MessageServiceImplTest

**유형**: `@ExtendWith(MockitoExtension.class)` (순수 단위)  
**Mock**: `MessageRepository`  
**대상**: `MessageServiceImpl`

| 그룹 | 테스트 | 검증 내용 |
|---|---|---|
| 날짜 포맷 | startDate 변환 | "20250101" → "20250101000000" |
| 날짜 포맷 | endDate 변환 | "20251231" → "20251231235959" |
| msgType 파싱 | 유효 숫자 "1" | Integer 1로 변환 |
| msgType 파싱 | 빈 문자열 "" | null로 처리 |
| msgType 파싱 | null | null로 처리 |
| msgType 파싱 | 숫자 아닌 값 "abc" | null로 처리 (예외 미발생) |
| recipient | 빈 문자열 "" | null로 처리 |
| recipient | 공백 " " | null로 처리 |
| recipient | 유효 값 "010" | "010" 그대로 전달 |
| msgTypeNm | 1~13, 99 각 코드 | SMS/LMS/MMS/알림톡/친구톡/AI알림톡/RCS SMS~이미지템플릿/기타 |
| statusNm | 0, 1/3/5/7, 2/4/6/8, 9 | 대기/처리중/전송완료/실패 |
| 응답 DTO | PagedResponseDto 필드 | content/totalElements/totalPages/currentPage/pageSize 정합성 |

---

### 3. AuthControllerTest

**유형**: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`  
**Mock**: `PasswordEncoder` (plain-text 비교), `TokenBlacklistService`, `CacheManager`

#### POST /api/auth/register

| 케이스 | 기대 응답 |
|---|---|
| 신규 사용자 등록 성공 | 200, userId/userUrl/message 반환 |
| 동일 userId+userUrl 중복 등록 | 400, "이미 등록된" 포함 error |
| userId 누락 | 400 |
| insertId 누락 | 400 |

#### POST /api/auth/token (API 타입 토큰 발급)

| 케이스 | 기대 응답 |
|---|---|
| DEMO_USER 정상 발급 | 200, accessToken/tokenType 반환 |
| 잘못된 비밀번호 | 400, "비밀번호" 포함 error |
| 존재하지 않는 사용자 | 400, "찾을 수 없습니다" 포함 error |
| userUrl 누락 | 400 |

#### POST /api/auth/login (웹 로그인)

| 케이스 | 기대 응답 |
|---|---|
| WEB_USER_01 정상 로그인 | 200, accessToken/userId/userType="2" 반환 |
| WEB_USER_02 정상 로그인 | 200 |
| 잘못된 비밀번호 | 400, "비밀번호" 포함 error |
| 존재하지 않는 사용자 | 400, "찾을 수 없습니다" 포함 error |
| API 타입(type=1) 계정으로 웹 로그인 시도 | 400 |
| userId 누락 | 400 |

#### POST /api/auth/logout

| 케이스 | 기대 응답 |
|---|---|
| 유효한 토큰으로 로그아웃 | 200, message/userId 반환 |
| 토큰 없이 요청 | 401 |

---

### 4. MessageControllerTest

**유형**: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`  
**Mock**: `PasswordEncoder`, `TokenBlacklistService`, `CacheManager`

#### POST /api/messages/search

| 그룹 | 케이스 | 기대 응답 |
|---|---|---|
| 성공 | 날짜만으로 조회 | 200, content/totalElements/totalPages/currentPage/pageSize 포함 |
| 성공 | status+msgType+recipient 전체 필터 | 200, content 배열 포함 |
| 성공 | type=1(API) 토큰으로 조회 | 200, content 배열 포함 |
| 성공 | page=0, size=5 페이지네이션 | 200, pageSize=5/currentPage=0 |
| 인증 실패 | 토큰 없이 요청 | 401 |
| 인증 실패 | 유효하지 않은 토큰 | 401 |
| 인증 실패 | 블랙리스트 토큰 | 401, "로그아웃된 토큰" error |
| 입력값 검증 | 시작일 > 종료일 | 400 |

---

### 5. MessageCacheTest

**유형**: `@SpringBootTest` + `@ActiveProfiles("test")`  
**Mock**: `MessageRepository`, `TokenBlacklistService`, `CacheManager`  
**캐시**: `ConcurrentMapCache`를 `CacheManager` Mock에 stub하여 실제 캐시 히트/미스 동작 검증

| 테스트 | 검증 내용 |
|---|---|
| 동일 조건 재조회 | Repository 1회만 호출 (2번 조회 → 캐시 히트) |
| 다른 날짜 범위 | 별도 캐시 키 → Repository 각각 호출 (2회) |
| 다른 status 조건 | 별도 캐시 키 → Repository 각각 호출 (2회) |
| 다른 page 조건 | 별도 캐시 키 → Repository 각각 호출 (2회) |

---

## 자주 묻는 질문

**Q. 실제 PostgreSQL이나 Redis 없이 테스트가 돌아가나요?**  
A. 네. PostgreSQL은 H2 인메모리 DB로, Redis는 Mockito Mock으로 대체하므로 Docker나 외부 서버 없이 바로 실행됩니다.

**Q. `MessageRepositoryImplTest`에서 `@ActiveProfiles("test")`를 안 쓰는 이유가 뭔가요?**  
A. `@DataJpaTest`는 JPA 계층만 띄우는 슬라이스 테스트입니다. `test` 프로파일을 활성화하면 `application-test.yml`의 `sql.init` 설정이 적용되어 `data-h2.sql`의 33건이 추가 삽입됩니다. 이 테스트는 `@BeforeEach`에서 직접 데이터를 제어하므로 프로파일을 사용하지 않고 `@TestPropertySource`로 필요한 속성만 오버라이드합니다.

**Q. 여러 `@SpringBootTest` 클래스를 한 번에 실행하면 왜 컨텍스트가 여러 개 뜨나요?**  
A. Spring은 `@MockBean` 조합이 다르면 별도 컨텍스트를 생성합니다. 현재 `AuthControllerTest`, `MessageControllerTest`, `MessageCacheTest`가 각각 다른 Mock 조합을 가지므로 최대 3개의 컨텍스트가 생성될 수 있습니다. 모두 같은 `jdbc:h2:mem:oms` DB를 공유하며, `schema-h2.sql`의 `IF NOT EXISTS`와 `data-h2.sql`의 `TRUNCATE → INSERT`로 충돌을 방지합니다.

**Q. 테스트에서 비밀번호가 BCrypt 해시 없이 평문으로 동작하는 이유는요?**  
A. H2 초기 데이터(`data-h2.sql`)에 plain-text 비밀번호가 저장되어 있습니다. 통합 테스트에서 `@MockBean PasswordEncoder`를 선언하고 `matches(raw, encoded)`가 두 값을 직접 비교하도록 stub하므로, 실제 BCrypt 해싱 없이 로그인 흐름을 검증할 수 있습니다.
