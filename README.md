# OMS API (Omni Message Service)

메시지 발송 이력 조회를 위한 REST API 서버입니다.
JWT 기반 인증, 사용자 관리, 메시지 이력 검색 기능을 제공합니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (JJWT 0.12.3) |
| Database | H2 In-Memory (MySQL Mode) |
| ORM | Spring Data JPA (Hibernate) |
| Documentation | Springdoc OpenAPI (Swagger UI 2.5.0) |
| SQL Logging | P6Spy 3.9.1 |
| Build | Gradle 8.x |

---

## 프로젝트 구조

```
oms/
├── src/
│   ├── main/
│   │   ├── java/com/dbass/oms/api/
│   │   │   ├── OmsApplication.java
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java          # CORS 설정
│   │   │   │   ├── SecurityConfig.java      # Spring Security 설정
│   │   │   │   └── SwaggerConfig.java       # Swagger 설정
│   │   │   ├── controller/
│   │   │   │   ├── ApiKeyController.java    # 인증 API (register/token/login/logout)
│   │   │   │   ├── MessageController.java   # 메시지 조회 API
│   │   │   │   └── HealthCheckController.java
│   │   │   ├── dto/                         # Request/Response DTO
│   │   │   ├── entity/
│   │   │   │   └── MessageLog.java          # TEST_SUBMIT_LOG 엔티티
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/
│   │   │   │   ├── MessageRepository.java
│   │   │   │   └── OmsUserRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java    # JWT 생성/검증
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserPrincipal.java
│   │   │   └── service/
│   │   │       ├── MessageService.java
│   │   │       ├── OmsUserService.java
│   │   │       └── impl/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── sql/ddl/
│   │           ├── schema-h2.sql            # 테이블 DDL
│   │           └── data-h2.sql             # 초기 데이터
│   └── test/
│       └── java/com/dbass/oms/api/
│           ├── OmsApplicationTests.java
│           └── controller/
│               └── AuthControllerTest.java  # 로그인/로그아웃 통합 테스트
├── build.gradle
└── README.md
```

---

## 실행 방법

### 로컬 실행 (개발)

사전 조건: Java 17 이상

```bash
# 빌드 후 실행
./gradlew clean build
./gradlew bootRun

# 또는 IntelliJ에서 OmsApplication.java 직접 실행
```

서버 기본 포트: **8080**

### 테스트 실행

```bash
./gradlew test
```

---

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `OMS_JWT_SECRET` | JWT 서명 시크릿 (32자 이상) | application.yml 참조 |
| `OMS_JWT_EXPIRES_MINUTES` | JWT 만료 시간(분) | 525600 (1년) |

---

## API 엔드포인트

### 인증 (인증 불필요)

#### 사용자 등록
```http
POST /api/auth/register
Content-Type: application/json

{
  "userId": "MY_SERVICE",
  "userUrl": "https://myservice.com",
  "userType": "1",
  "userPassword": "mypassword",
  "insertId": "admin"
}
```

#### JWT 발급 (API 타입 전용)
```http
POST /api/auth/token
Content-Type: application/json

{
  "userId": "DEMO_USER",
  "userUrl": "https://example.com",
  "userPassword": "password"
}
```

#### 웹 로그인 (웹 타입 전용, user_type=2)
```http
POST /api/auth/login
Content-Type: application/json

{
  "userId": "WEB_USER_01",
  "userPassword": "password1"
}
```

응답:
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresInMinutes": 525600,
  "userId": "WEB_USER_01",
  "userType": "2"
}
```

### 인증 (JWT 필요)

#### 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {token}
```

#### 메시지 이력 조회
```http
POST /api/messages/search
Authorization: Bearer {token}
Content-Type: application/json

{
  "startDate": "20250101",
  "endDate": "20251231",
  "status": 2,
  "msgType": "1",
  "recipient": "010"
}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | String | ✅ | 조회 시작일 (yyyyMMdd) |
| endDate | String | ✅ | 조회 종료일 (yyyyMMdd) |
| status | Integer | - | 상태 코드 |
| msgType | String | - | 메시지 유형 코드 |
| recipient | String | - | 수신 번호 (부분 일치) |

### 공통

#### 헬스체크
```http
GET /api/health
```

---

## 사용자 타입

| user_type | 설명 | 접근 가능 경로 |
|-----------|------|---------------|
| `1` (API) | REST API 연동용 | 전체 API |
| `2` (Web) | 웹 조회 페이지용 | `/api/auth/**`, `/api/health`, `/api/messages/**` |

---

## 메시지 유형 코드

| 코드 | 유형 |
|------|------|
| 1 | SMS |
| 2 | LMS |
| 3 | MMS |
| 6 | 알림톡 |
| 7 | 친구톡 |
| 8 | AI알림톡 |
| 9 | RCS SMS |
| 10 | RCS LMS |
| 11 | RCS MMS |
| 12 | RCS 템플릿 |
| 13 | RCS 이미지템플릿 |

## 전송 상태 코드

| 코드 | 상태 |
|------|------|
| 0 | 대기 |
| 1,3,5,7 | 처리중 |
| 2,4,6,8 | 전송완료 |
| 9 | 실패 |

---

## 초기 계정

서버 시작 시 `data-h2.sql`로 자동 생성됩니다.

| userId | userUrl | userType | password | 용도 |
|--------|---------|----------|----------|------|
| DEMO_USER | https://example.com | 1 (API) | password | API 테스트 |
| WEB_USER_01 | https://web.oms.local | 2 (Web) | password1 | 웹 조회 테스트 |
| WEB_USER_02 | https://web.oms.local | 2 (Web) | password2 | 웹 조회 테스트 |

> H2 In-Memory DB 특성상 서버 재시작 시 초기화됩니다.

---

## 개발 도구

### Swagger UI
- URL: http://localhost:8080/swagger-ui.html
- 모든 API를 브라우저에서 직접 테스트 가능

### H2 콘솔
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:oms`
- User: `sa` / Password: (빈 값)

---

## JWT 인증 흐름

```
1. POST /api/auth/login  →  { accessToken, userId, userType }
2. 이후 요청 헤더에 포함  →  Authorization: Bearer {accessToken}
3. POST /api/auth/logout →  클라이언트에서 토큰 삭제 (서버는 stateless)
```

---

## Docker

`DOCKER.md` 참고. 실행 전 `.env.example`을 복사해 `.env`를 생성해야 합니다.

```bash
cp .env.example .env
# .env 열어서 OMS_JWT_SECRET 입력 후 docker-compose up --build
```

---

## 테스트

JUnit 5 + Spring Boot Test + Mockito 기반 테스트 63건 (전 구간 커버)

| 테스트 클래스 | 유형 | 건수 |
|---|---|---|
| `MessageRepositoryImplTest` | Repository (QueryDSL 슬라이스) | 15 |
| `MessageServiceImplTest` | Service (순수 단위) | 17 |
| `AuthControllerTest` | Controller 통합 (인증 API) | 23 |
| `MessageControllerTest` | Controller 통합 (메시지 조회 API) | 8 |
| `MessageCacheTest` | Cache 동작 통합 | 4 |

자세한 테스트 항목, 설계 의도, 실행 가이드는 **[TEST.md](TEST.md)** 참조

---

## 업데이트 내역

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
- 전체 조회 후, 페이지에서 구분 -> 지정 단위 별로 페이지 조회 기능으로 변경
  : 서버 전체 조회 대비 부하 감소

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
- 날짜 선택 컴포넌트를 커스텀 DatePicker → **Ant Design RangePicker**로 교체
  - 시작일/종료일 하나의 컴포넌트로 통합
  - 시작일 > 종료일 선택 자동 차단
  - 수기 입력 시 `-` 자동 삽입 (mask 모드)
  - 한국어 locale 적용
- 조회 결과 **100건 단위 페이지네이션** 추가
- 총 건수 3자리 콤마 포맷 적용
