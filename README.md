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
| Build | Gradle 8.x |

---

## 실행 방법

### 사전 조건

- Java 17 이상
- Redis 실행 중 (로컬 또는 Docker)
- `.env` 파일 설정 완료

### 로컬 실행

```bash
./gradlew clean build
./gradlew bootRun
```

서버 기본 포트: **8080**

### Docker 실행

```bash
cp .env.example .env
# .env 열어서 환경변수 입력 후:
docker-compose up --build
```

자세한 내용은 [DOCKER.md](DOCKER.md) 참고.

### 테스트 실행

```bash
./gradlew test
```

---

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `OMS_BASE_URL` | 서버 외부 접근 URL | `http://localhost` |
| `OMS_PORT` | 서버 포트 | `8080` |
| `OMS_JWT_SECRET` | JWT 서명 시크릿 (32자 이상) | - |
| `OMS_JWT_EXPIRES_MINUTES` | Access Token 만료 시간(분) | `30` |
| `OMS_JWT_REFRESH_EXPIRES_MINUTES` | Refresh Token 만료 시간(분) | `10080` |
| `DB_USERNAME` | PostgreSQL 접속 계정 | - |
| `DB_PASSWORD` | PostgreSQL 접속 비밀번호 | - |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 출처 | - |

---

## API 문서

서버 실행 후 Swagger UI에서 전체 API 스펙 확인:

```
http://localhost:8080/swagger-ui.html
```

### JWT 인증 흐름

```
1. POST /api/auth/login      →  accessToken (30분) + refreshToken (7일) 발급
2. 이후 요청                 →  Authorization: Bearer {accessToken}
3. accessToken 만료 (401)   →  POST /api/auth/refresh → 신규 토큰 쌍 발급 (Rotation)
4. POST /api/auth/logout     →  refreshToken 삭제, accessToken 블랙리스트 등록
```

### 사용자 타입별 접근 권한

| user_type | 설명 | 접근 범위 |
|-----------|------|-----------|
| `99` | 관리자 | 전체 API + 에이전트 처리 |
| `1` | API 연동 | 전체 API |
| `2` | 발송 가능 | 인증 + 메시지 발송/조회 |
| `3` | 일반 | 인증 + 메시지 조회 |

---

## 개발 도구

| 도구 | URL |
|------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Spring Boot Admin | http://localhost:8080/admin |
| H2 Console (테스트) | http://localhost:8080/h2-console |

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [DOCKER.md](DOCKER.md) | Docker Compose 실행, 환경변수 설정 |
| [ACTUATOR.md](ACTUATOR.md) | Spring Boot Actuator 엔드포인트 가이드 |
| [REDIS.md](REDIS.md) | Redis 관제 및 캐시/토큰 동작 흐름 |
| [TEST.md](TEST.md) | 테스트 현황, 실행 방법, 클래스별 상세 |
| [UPDATE_LOG.md](UPDATE_LOG.md) | 전체 업데이트 내역 |