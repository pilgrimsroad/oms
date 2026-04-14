# OMS API (Omni Message Service)

메시지 발송 요청 및 이력 조회를 위한 REST API 서버입니다.
JWT 기반 인증, 역할 기반 접근 제어, 메시지 발송 요청, 이력 검색 기능을 제공합니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (JJWT 0.12.3) |
| Database | PostgreSQL (운영) / H2 In-Memory (테스트) |
| Cache / Token | Redis (메시지 캐시, JWT 블랙리스트, Refresh Token) |
| ORM | Spring Data JPA (Hibernate) + QueryDSL 5.1.0 |
| Monitoring | Spring Boot Actuator + Spring Boot Admin 3.2.3 |
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
│   │   │   ├── OmsApplication.java          # @EnableAdminServer 포함
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── CacheConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ApiKeyController.java    # 인증 API (register/token/login/logout/refresh)
│   │   │   │   ├── MessageController.java   # 메시지 조회/발송 API
│   │   │   │   └── AgentController.java     # 에이전트 처리 API (로컬 테스트용)
│   │   │   ├── dto/
│   │   │   │   ├── MessageSendRequestDto.java
│   │   │   │   ├── MessageSendResponseDto.java
│   │   │   │   ├── RefreshTokenRequestDto.java
│   │   │   │   └── ...
│   │   │   ├── entity/
│   │   │   │   ├── MessageLog.java          # TEST_SUBMIT_LOG 엔티티
│   │   │   │   └── OmsUser.java
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/
│   │   │   │   ├── MessageRepository.java
│   │   │   │   ├── MessageRepositoryImpl.java  # QueryDSL 동적 쿼리
│   │   │   │   └── OmsUserRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserPrincipal.java
│   │   │   └── service/
│   │   │       ├── MessageService.java
│   │   │       ├── OmsUserService.java
│   │   │       ├── TokenBlacklistService.java
│   │   │       └── impl/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── sql/ddl/
│   │           ├── schema-postgresql.sql
│   │           ├── schema-h2.sql
│   │           ├── data-postgresql.sql
│   │           └── data-h2.sql
│   └── test/
│       └── java/com/dbass/oms/api/
│           ├── config/TestConfig.java
│           └── controller/
│               ├── AuthControllerTest.java
│               ├── MessageControllerTest.java
│               ├── MessageSendControllerTest.java
│               └── RefreshTokenControllerTest.java
│           └── repository/
│               └── MessageRepositoryImplTest.java
│           └── service/
│               ├── MessageServiceImplTest.java
│               └── MessageCacheTest.java
├── build.gradle
├── docker-compose.yml
├── docker-compose.full.yml
├── .env
└── README.md
```

---

## 실행 방법

### 로컬 실행 (개발)

사전 조건: Java 17 이상, Redis 실행 중

```bash
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
| `OMS_BASE_URL` | 서버 외부 접근 URL (포트 제외) | `http://localhost` |
| `OMS_PORT` | 서버 포트 | `8080` |
| `FRONT_PORT` | 프론트엔드 포트 (Docker) | `80` |
| `OMS_JWT_SECRET` | JWT 서명 시크릿 (32자 이상) | application.yml 참조 |
| `OMS_JWT_EXPIRES_MINUTES` | Access Token 만료 시간(분) | `30` |
| `OMS_JWT_REFRESH_EXPIRES_MINUTES` | Refresh Token 만료 시간(분) | `10080` (7일) |
| `DB_USERNAME` | PostgreSQL 접속 계정 | - |
| `DB_PASSWORD` | PostgreSQL 접속 비밀번호 | - |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 출처 | - |

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

#### JWT 발급 (API 타입 전용, user_type=1)
```http
POST /api/auth/token
Content-Type: application/json

{
  "userId": "DEMO_USER",
  "userUrl": "https://example.com",
  "userPassword": "password"
}
```

응답:
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresInMinutes": 30,
  "userId": "DEMO_USER",
  "userType": "1",
  "refreshToken": "uuid-string"
}
```

#### 웹 로그인 (웹 타입 전용, user_type=2,3,99)
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
  "expiresInMinutes": 30,
  "userId": "WEB_USER_01",
  "userType": "2",
  "refreshToken": "uuid-string"
}
```

### 인증 (JWT 필요)

#### Access Token 갱신 (Refresh Token 사용)
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "uuid-string"
}
```

응답:
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresInMinutes": 30,
  "refreshToken": "new-uuid-string"
}
```

> Refresh Token Rotation: 호출 시마다 기존 토큰 삭제 + 신규 토큰 발급

#### 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {token}
Content-Type: application/json

{
  "refreshToken": "uuid-string"
}
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
  "recipient": "010",
  "page": 0,
  "size": 100
}
```

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | String | ✅ | 조회 시작일 (yyyyMMdd) |
| endDate | String | ✅ | 조회 종료일 (yyyyMMdd) |
| status | Integer | - | 상태 코드 |
| msgType | String | - | 메시지 유형 코드 |
| recipient | String | - | 수신 번호 (부분 일치) |
| page | Integer | - | 페이지 번호 (0부터, 기본 0) |
| size | Integer | - | 페이지 크기 (기본 100) |

#### 메시지 발송 요청
```http
POST /api/messages/send
Authorization: Bearer {token}
Content-Type: application/json

{
  "msgType": 1,
  "callbackNum": "01000000000",
  "rcptData": "01012345678",
  "subject": "제목 (선택)",
  "message": "발송 내용",
  "scheduleTime": "20260101120000"
}
```

응답:
```json
{
  "msgId": 1,
  "msgType": 1,
  "rcptData": "01012345678",
  "status": 0,
  "requestedAt": "20260414120000",
  "message": "발송 요청이 완료되었습니다."
}
```

> TEST_SUBMIT_LOG 테이블에 status=0(대기)으로 insert됩니다.

#### 발송 대기 건수 조회
```http
GET /api/agent/pending-count
Authorization: Bearer {token}
```

응답:
```json
{
  "pendingCount": 3
}
```

#### 에이전트 처리 (로컬 테스트용)
```http
POST /api/agent/process
Authorization: Bearer {token}
```

응답:
```json
{
  "processedCount": 3,
  "message": "3건 처리 완료"
}
```

> 발송 대기(status=0) 건을 전송완료(status=2)로 일괄 업데이트합니다.  
> 실제 발송 에이전트 서버 역할을 로컬에서 시뮬레이션하기 위한 용도입니다.

### 모니터링 (인증 불필요)

#### 헬스체크 (Actuator)
```http
GET /actuator/health
```

#### Spring Boot Admin
```
http://localhost:8080/admin
```
> 인스턴스 상태, 메트릭, 로거, 환경변수, 스레드 덤프 등 실시간 모니터링

---

## 사용자 타입

| user_type | 설명 | 접근 가능 경로 |
|-----------|------|---------------|
| `99` (관리자) | 관리자 | 전체 API + 에이전트 처리 |
| `1` (API) | REST API 연동용 | 전체 API |
| `2` (발송가능) | 발송 및 조회 | `/api/auth/**`, `/api/messages/**` |
| `3` (일반) | 조회 전용 | `/api/auth/**`, `/actuator/health`, `/api/messages/search` |

---

## 메시지 유형 코드

| 코드 | 유형 | 바이트 제한 |
|------|------|-------------|
| 1 | SMS | 80 byte |
| 2 | LMS | 2,000 byte |
| 3 | MMS | 2,000 byte |
| 6 | 알림톡 | - |
| 7 | 친구톡 | - |
| 8 | AI알림톡 | - |
| 9 | RCS SMS | - |
| 10 | RCS LMS | - |
| 11 | RCS MMS | - |
| 12 | RCS 템플릿 | - |
| 13 | RCS 이미지템플릿 | - |

> 바이트 계산 기준: 한글 2byte, 영문/숫자/특수문자 1byte

## 전송 상태 코드

| 코드 | 상태 |
|------|------|
| 0 | 대기 |
| 1,3,5,7 | 처리중 |
| 2,4,6,8 | 전송완료 |
| 9 | 실패 |

---

## 초기 계정

서버 시작 시 `data-h2.sql`로 자동 생성됩니다. (PostgreSQL은 `data-postgresql.sql` 수동 적용)

| userId | userType | 용도 |
|--------|----------|------|
| ADMIN | 99 (관리자) | 관리자 계정 |
| DEMO_USER | 1 (API) | API 테스트 |
| WEB_USER_01 | 2 (발송가능) | 웹 발송 테스트 |
| WEB_USER_02 | 2 (발송가능) | 웹 발송 테스트 |

> 비밀번호는 BCrypt 해시로 저장됩니다. 최초 계정 등록은 `/api/auth/register` API를 사용하세요.

---

## 개발 도구

### Swagger UI
- URL: http://localhost:8080/swagger-ui.html

### H2 콘솔 (테스트 환경)
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:oms`
- User: `sa` / Password: (빈 값)

### Spring Boot Admin
- URL: http://localhost:8080/admin

---

## JWT 인증 흐름

```
1. POST /api/auth/login  →  { accessToken (30분), refreshToken (7일) }
2. 이후 요청 헤더에 포함  →  Authorization: Bearer {accessToken}
3. accessToken 만료(401) →  POST /api/auth/refresh { refreshToken }
                          →  신규 accessToken + 신규 refreshToken 발급 (Rotation)
4. POST /api/auth/logout →  refreshToken Redis에서 삭제, accessToken 블랙리스트 등록
```

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [DOCKER.md](DOCKER.md) | Docker Compose 실행, 환경변수 설정 |
| [ACTUATOR.md](ACTUATOR.md) | Spring Boot Actuator 엔드포인트 가이드 |
| [REDIS.md](REDIS.md) | Redis 관제 및 캐시/토큰 동작 흐름 |
| [TEST.md](TEST.md) | 테스트 현황, 실행 방법, 클래스별 상세 |
| [UPDATE_LOG.md](UPDATE_LOG.md) | 전체 업데이트 내역 |

---

## Docker

`DOCKER.md` 참고. 실행 전 `.env.example`을 복사해 `.env`를 생성해야 합니다.

```bash
cp .env.example .env
# .env 열어서 환경변수 입력 후 docker-compose up --build
```

---

## 테스트

JUnit 5 + Spring Boot Test + Mockito 기반 테스트 82건 (전 구간 커버)

| 테스트 클래스 | 유형 | 건수 |
|---|---|---|
| `MessageRepositoryImplTest` | Repository (QueryDSL 슬라이스) | 15 |
| `MessageServiceImplTest` | Service (순수 단위) | 17 |
| `AuthControllerTest` | Controller 통합 (인증 API) | 23 |
| `MessageControllerTest` | Controller 통합 (메시지 조회 API) | 8 |
| `MessageCacheTest` | Cache 동작 통합 | 4 |
| `MessageSendControllerTest` | Controller 통합 (발송 요청/에이전트) | 10 |
| `RefreshTokenControllerTest` | Controller 통합 (Refresh Token) | 9 |

> 총 82건. 자세한 내용은 **[TEST.md](TEST.md)** 참조

자세한 테스트 항목, 설계 의도, 실행 가이드는 **[TEST.md](TEST.md)** 참조

업데이트 내역은 **[UPDATE_LOG.md](UPDATE_LOG.md)** 참조
