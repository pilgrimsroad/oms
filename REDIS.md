# Redis 관제 가이드

OMS에서 Redis는 JWT 블랙리스트(로그아웃 토큰 차단)와 메시지 조회 캐싱에 사용됩니다.
Redis Insight를 통해 저장된 키, 캐시 히트, 명령어 흐름을 실시간으로 확인할 수 있습니다.

---

## 접속

| 항목 | 내용 |
|------|------|
| URL | http://localhost:8001 |
| 실행 조건 | Docker Compose 실행 중 (`docker compose ... up`) |

---

## 최초 연결 (Add Database)

1. http://localhost:8001 접속
2. **Add Database** 클릭
3. 아래 값 입력:

| 항목 | 값 |
|------|----|
| Host | `redis` |
| Port | `6379` |
| Database Alias | `oms-redis` (자유 입력) |

4. **Add Redis Database** 클릭

> **주의:** Host를 `127.0.0.1`이나 `localhost`로 입력하면 연결되지 않습니다.
> Redis Insight 컨테이너 기준으로 Redis는 `redis`(컨테이너 이름)로 통신합니다.

---

## 주요 기능

### 1. Browser (키 탐색)

좌측 메뉴 **Browser** 탭에서 현재 Redis에 저장된 모든 키를 조회합니다.

#### OMS에서 사용하는 키 패턴

| 키 패턴 | 설명 | TTL |
|---------|------|-----|
| `blacklist:{JWT 토큰}` | 로그아웃된 토큰 차단용 | 토큰 남은 만료시간 |
| `messages::{검색 조건}` | 메시지 조회 캐시 | 5분 |

#### 확인 방법

- **로그아웃 블랙리스트 확인**
  - 웹에서 로그아웃 후 Browser에서 `blacklist:*` 필터링
  - 해당 키 클릭 → Value: `1`, TTL 남은 시간 표시

- **메시지 캐시 확인**
  - 메시지 조회 후 Browser에서 `messages::*` 필터링
  - 키 클릭 → Value에 JSON 형태의 조회 결과 확인
  - 동일 조건 재조회 시 TTL이 줄어드는지 확인 (캐시 히트)

---

### 2. Profiler (실시간 명령어 모니터링)

좌측 메뉴 **Profiler** 탭에서 Redis로 들어오는 모든 명령어를 실시간으로 확인합니다.

#### 사용 방법

1. **Profiler** 탭 클릭
2. **Start** 버튼 클릭
3. 웹에서 로그인 / 조회 / 로그아웃 동작 수행
4. 명령어 스트림 확인

#### 주요 확인 포인트

| 동작 | 예상 명령어 |
|------|------------|
| 메시지 조회 (최초) | `GET messages::...` → miss → `SET messages::...` |
| 메시지 조회 (재조회) | `GET messages::...` → hit (SET 없음) |
| 로그아웃 | `SET blacklist:{token} 1 EX {초}` |
| 로그아웃 후 API 재요청 | `EXISTS blacklist:{token}` |

> Profiler는 실행 중 Redis 성능에 영향을 줄 수 있으므로 확인 후 **Stop** 하세요.

---

### 3. Memory Analysis (메모리 분석)

좌측 메뉴 **Memory Analysis** 탭에서 키별 메모리 사용량을 분석합니다.

- 캐시 키가 비정상적으로 많이 쌓인 경우 확인
- 키 타입별 메모리 분포 확인

---

## 캐시 수동 삭제

테스트 중 캐시를 강제로 만료시키고 싶을 때:

### Redis Insight에서 삭제

1. Browser → 키 선택
2. 우측 상단 **Delete** (휴지통 아이콘) 클릭

### CLI에서 삭제

```bash
# Docker 컨테이너 내 redis-cli 접속
docker exec -it oms-redis redis-cli

# 메시지 캐시 전체 삭제
KEYS messages::*
DEL messages::{키_전체_이름}

# 블랙리스트 확인
KEYS blacklist:*

# 특정 키 TTL 확인
TTL blacklist:{token}

# 전체 키 삭제 (주의: 모든 데이터 삭제)
FLUSHALL
```

> 컨테이너 이름은 `docker ps`로 확인 (`oms-redis`가 기본값).

---

## 캐시 동작 흐름

```
[첫 번째 조회]
브라우저 → POST /api/messages/search
           → Redis GET messages::... → miss
           → PostgreSQL 쿼리 실행
           → Redis SET messages::... (TTL 5분)
           → 응답 반환

[5분 이내 동일 조건 재조회]
브라우저 → POST /api/messages/search
           → Redis GET messages::... → hit
           → 응답 반환 (DB 쿼리 없음)

[로그아웃 후 토큰 재사용 시도]
클라이언트 → API 요청 (Authorization: Bearer {token})
              → Redis EXISTS blacklist:{token} → true
              → 401 Unauthorized
```

---

## 로컬 개발 환경에서 Redis 연결

Docker 없이 로컬에서 Spring Boot를 실행하는 경우 Redis가 별도로 필요합니다.

### 옵션 A: Redis만 Docker로 실행

```bash
docker run -d --name local-redis -p 6379:6379 redis:7
```

### 옵션 B: Homebrew (Mac)

```bash
brew install redis
brew services start redis
```

> `application.yml` 기본값: `REDIS_HOST=localhost`, `REDIS_PORT=6379`
