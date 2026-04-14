# Spring Boot Actuator 가이드

OMS는 Spring Boot Actuator를 통해 애플리케이션 상태, 메트릭, 로거 수준 등을 모니터링합니다.
Spring Boot Admin UI(`/admin`)에서 이 정보들을 시각적으로 확인할 수 있습니다.

---

## 접속

| 항목 | URL |
|------|-----|
| Actuator 루트 | http://localhost:8080/actuator |
| Spring Boot Admin UI | http://localhost:8080/admin |

> Actuator 엔드포인트는 인증 없이 접근 가능합니다 (내부망/Docker 환경 전제).

---

## 노출된 엔드포인트

`application.yml`에서 아래 엔드포인트를 노출하도록 설정되어 있습니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, loggers, caches, threaddump, env
```

| 엔드포인트 | URL | 설명 |
|------------|-----|------|
| `health` | `/actuator/health` | 애플리케이션 및 DB, Redis 상태 |
| `info` | `/actuator/info` | 앱 정보 (버전 등) |
| `metrics` | `/actuator/metrics` | JVM, HTTP, 시스템 메트릭 목록 |
| `loggers` | `/actuator/loggers` | 패키지별 로그 레벨 조회/변경 |
| `caches` | `/actuator/caches` | 등록된 캐시 목록 |
| `threaddump` | `/actuator/threaddump` | 현재 스레드 상태 덤프 |
| `env` | `/actuator/env` | 환경변수 및 설정값 |

---

## 엔드포인트 상세

### `/actuator/health`

애플리케이션, DB, Redis 연결 상태를 한 번에 확인합니다.

```bash
curl http://localhost:8080/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "redis": { "status": "UP", "details": { "version": "7.x" } },
    "diskSpace": { "status": "UP" }
  }
}
```

> Docker healthcheck에서도 이 엔드포인트를 사용합니다.

---

### `/actuator/metrics`

JVM 메모리, HTTP 요청 수, GC 횟수 등 다양한 메트릭을 조회합니다.

```bash
# 메트릭 목록 조회
curl http://localhost:8080/actuator/metrics

# 특정 메트릭 조회 (JVM 힙 메모리 사용량)
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP 요청 총 건수
curl http://localhost:8080/actuator/metrics/http.server.requests
```

주요 메트릭:

| 메트릭 키 | 설명 |
|-----------|------|
| `jvm.memory.used` | JVM 힙/비힙 메모리 사용량 |
| `jvm.memory.max` | JVM 최대 메모리 |
| `jvm.gc.pause` | GC 정지 시간 |
| `http.server.requests` | HTTP 요청 건수/응답시간 |
| `process.cpu.usage` | 프로세스 CPU 사용률 |
| `system.cpu.usage` | 시스템 CPU 사용률 |

---

### `/actuator/loggers`

패키지별 로그 레벨을 조회하고, **서버 재시작 없이** 런타임에서 변경할 수 있습니다.

```bash
# 전체 로거 목록
curl http://localhost:8080/actuator/loggers

# 특정 패키지 로그 레벨 조회
curl http://localhost:8080/actuator/loggers/com.dbass.oms

# 특정 패키지 로그 레벨 변경 (DEBUG로 상향)
curl -X POST http://localhost:8080/actuator/loggers/com.dbass.oms \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# 로그 레벨 원복 (null = 상위 패키지 레벨 상속)
curl -X POST http://localhost:8080/actuator/loggers/com.dbass.oms \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": null}'
```

> 운영 중 특정 기능에서 오류가 발생할 때, 재배포 없이 해당 패키지만 DEBUG로 올려 로그를 확인할 수 있습니다.

---

### `/actuator/caches`

Spring Cache에 등록된 캐시 목록을 확인하고, 특정 캐시를 수동으로 삭제할 수 있습니다.

```bash
# 캐시 목록 조회
curl http://localhost:8080/actuator/caches

# 특정 캐시 삭제 (메시지 조회 캐시 전체 무효화)
curl -X DELETE http://localhost:8080/actuator/caches/messages
```

> OMS에서 사용하는 캐시: `messages` (메시지 조회 결과, TTL 5분)

---

### `/actuator/threaddump`

현재 JVM에서 실행 중인 스레드 전체의 상태를 조회합니다.

```bash
curl http://localhost:8080/actuator/threaddump
```

- 스레드 이름, 상태(RUNNABLE / WAITING / BLOCKED), 스택 트레이스 포함
- 성능 이슈나 데드락 의심 시 확인용

---

### `/actuator/env`

`application.yml`, 환경변수, 시스템 프로퍼티 등 현재 적용된 모든 설정값을 조회합니다.

```bash
# 전체 환경변수 목록
curl http://localhost:8080/actuator/env

# 특정 프로퍼티 조회
curl http://localhost:8080/actuator/env/server.port
```

> 민감한 값(비밀번호, 시크릿 등)은 `******`로 마스킹되어 표시됩니다.

---

## Spring Boot Admin UI 활용

Spring Boot Admin(`/admin`)은 위 Actuator 데이터를 시각적으로 제공합니다.

### 접속 방법

1. http://localhost:8080/admin 접속
2. 좌측 인스턴스 목록에서 **oms-api** 클릭

### 주요 탭

| 탭 | 내용 |
|----|------|
| **Details** | health 상태, 메모리/CPU 게이지, 업타임 |
| **Metrics** | 메트릭 검색 및 실시간 수치 확인 |
| **Loggers** | 패키지별 로그 레벨 UI에서 직접 변경 |
| **Caches** | 등록된 캐시 목록 및 삭제 버튼 |
| **Threads** | 스레드 목록 및 덤프 |
| **Environment** | 현재 적용된 설정값 |

---

## Docker 환경에서의 서비스 URL 설정

Docker 컨테이너 내부에서 Spring Boot Admin 클라이언트가 자신의 URL을 등록할 때,
컨테이너 ID가 아닌 외부 접근 가능한 URL로 등록되도록 환경변수로 명시합니다.

`.env` 설정:
```
OMS_BASE_URL=http://localhost
OMS_PORT=8080
```

`application.yml` 설정:
```yaml
spring:
  boot:
    admin:
      client:
        url: ${OMS_BASE_URL}:${OMS_PORT}/admin
        instance:
          service-url: ${OMS_BASE_URL}:${OMS_PORT}
```

> `OMS_BASE_URL`에 포트를 포함하지 않고, `OMS_PORT`를 별도 변수로 분리하여 포트 변경 시 한 곳에서만 수정합니다.
