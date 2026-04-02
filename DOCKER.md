# Docker 실행 가이드

백엔드(oms)와 프론트엔드(oms_front)를 Docker로 통합 실행하는 방법입니다.

---

## 구성

```
docker-compose
├── redis          (Redis 7)        →  포트 6379
├── redis-insight  (관제 UI)         →  포트 8001
├── oms-api        (Spring Boot)    →  포트 8080
└── oms-front      (nginx)          →  포트 80
       └── /api/* 요청은 oms-api:8080으로 프록시
```

---

## 사전 준비

### 1. .env 파일 생성

`oms/` 디렉터리에 `.env` 파일 생성 (`.env.example` 참고):

```bash
cp .env.example .env
```

`.env` 파일 편집:

```env
OMS_JWT_SECRET=여기에-32자-이상의-시크릿-키-입력
OMS_JWT_EXPIRES_MINUTES=525600
```

> `.env`는 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

### 2. 디렉터리 구조 확인

```
project/
├── oms/               ← docker-compose.yml 위치
└── oms_front/
```

---

## 실행 방식

### A. 통합 실행 (백엔드 + 프론트 한번에)

```bash
cd oms
docker compose -f docker-compose.full.yml up --build
```

| 서비스 | 접속 URL |
|--------|---------|
| 웹 페이지 | http://localhost |
| 백엔드 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Redis Insight | http://localhost:8001 |

> Redis Insight 최초 접속 시 **Add Database** → Host: `redis`, Port: `6379` 입력

### B. 분리 실행 (각각 독립 운영)

**백엔드만 실행:**
```bash
cd oms
docker compose up --build
```

**프론트만 실행** (백엔드가 8080에 떠 있어야 함):
```bash
cd oms_front
docker compose up --build
```

> 프론트 컨테이너는 `host.docker.internal:8080`으로 백엔드를 찾습니다.
> Mac/Windows Docker Desktop 환경에서 동작합니다.

---

## 관리 명령어

```bash
# 백그라운드 실행 (로그 없이 실행)
docker compose -f docker-compose.full.yml up -d --build

# 포그라운드 실행 (로그 보면서 실행)
docker compose -f docker-compose.full.yml up --build

# 중지
docker compose -f docker-compose.full.yml down

# 상태 확인
docker compose -f docker-compose.full.yml ps
```

### 로그 확인

```bash
# 전체 로그 (실시간)
docker compose -f docker-compose.full.yml logs -f

# 특정 서비스만
docker compose -f docker-compose.full.yml logs -f oms-api
docker compose -f docker-compose.full.yml logs -f oms-front
docker compose -f docker-compose.full.yml logs -f redis

# 현재까지 로그만 출력 (-f 없이)
docker compose -f docker-compose.full.yml logs oms-api
```

### 특정 서비스만 재빌드

코드 변경 시 전체 재빌드 없이 변경된 서비스만 재빌드 가능합니다.

```bash
# 프론트만 재빌드
docker compose -f docker-compose.full.yml up -d --build oms-front

# 백엔드만 재빌드
docker compose -f docker-compose.full.yml up -d --build oms-api
```

---

## 동작 방식

```
개발 환경 (npm run dev)
  브라우저(5173) → axios → http://localhost:8080/api

Docker 환경
  브라우저(80) → nginx → /api/* → http://oms-api:8080
                        → /     → React 정적 파일
```

`VITE_API_URL` 환경변수로 API 주소를 제어합니다.
- 개발: 미설정 → `http://localhost:8080` 직접 호출
- Docker: `VITE_API_URL=''`(빈 값) → nginx 프록시 경유

---

## 운영 환경과 로컬 환경 비교

| 구분 | 로컬 개발 | Docker |
|------|-----------|--------|
| 접속 방식 | localhost:포트 직접 | localhost:포트 직접 (포트 포워딩) |
| 운영 환경 | 도메인 + 로드밸런서 (80/443) | - |
| DB 호스트 | localhost | host.docker.internal |
| Redis 호스트 | localhost | redis (컨테이너 이름) |
| 개발 권장 | IntelliJ + npm run dev | 최종 확인 / 배포 검증 |

> Docker 컨테이너끼리는 포트 없이 **컨테이너 이름**으로 통신합니다.
> 로컬 PostgreSQL 연결 시 컨테이너 내부에서는 `host.docker.internal:5432`를 사용해야 합니다.

---

## 주의사항

- **PostgreSQL 사용 시** 로컬 DB가 실행 중이어야 하며, `DB_HOST=host.docker.internal`로 연결됩니다.
- `oms-api`는 `redis` 헬스체크 통과 후 시작되고, `oms-front`는 `oms-api` 헬스체크 통과 후 자동 시작됩니다.
- `.env` 파일은 절대 Git에 커밋하지 마세요.
