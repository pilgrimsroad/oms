# Omni Message Service (OMS) API

메시지 발송/이력 조회를 위한 REST API 프로젝트입니다.  
JWT 기반 인증과 검색/조회 API를 제공합니다.

## 기술 스택
- Java 17, Spring Boot 3.2.5, Spring Security (JWT)
- H2 (In-Memory), Spring Data JPA (Hibernate)
- Gradle, Swagger UI

## 실행
```bash
./gradlew clean build
./gradlew bootRun
```

## 주요 엔드포인트
- `POST /api/auth/register` 사용자 등록
- `POST /api/auth/token` JWT 발급
- `POST /api/messages/search` 메시지 이력 조회
- `GET /api/health` 헬스체크

## 인증
- `Authorization: Bearer {JWT}`

## 기본 계정
- `DEMO_USER` / `password`
# Omni Message Service(OMS) API

메시지 발송/이력 조회를 위한 REST API 프로젝트입니다.  
JWT 기반 인증, 사용자 등록, 메시지 이력 조회 기능을 제공합니다.

## 기술 스택
- **Java**: 17
- **Framework**: Spring Boot 3.2.5
- **Security**: Spring Security (JWT 인증)
- **Database**: H2 (In-Memory)
- **Data Access**: Spring Data JPA (Hibernate)
- **Documentation**: Springdoc OpenAPI (Swagger)
- **Build Tool**: Gradle

## 프로젝트 구조
```
oms/
├── src/main/java/com/dbass/oms/api/    # API 소스
├── src/main/resources/
│   ├── application.yml                 # 애플리케이션 설정
│   └── sql/                            # SQL 스크립트
├── build.gradle
└── README.md
```

## 실행 방법
```bash
./gradlew clean build
./gradlew bootRun
```

## JWT 인증 방식
- Header: `Authorization: Bearer {JWT}`
- 기본 만료: 1년 (525,600분)

## 샘플 요청
```http
POST /api/auth/register
Content-Type: application/json

{
  "userId": "DEMO_USER",
  "userUrl": "https://example.com",
  "userType": "1",
  "userPassword": "password",
  "insertId": "portfolio"
}
```

```http
POST /api/auth/token
Content-Type: application/json

{
  "userId": "DEMO_USER",
  "userUrl": "https://example.com",
  "userPassword": "password"
}
```

```http
POST /api/messages/search
Content-Type: application/json
Authorization: Bearer your-jwt-token

{
  "startDate": "20250101",
  "endDate": "20250131",
  "page": 0,
  "size": 20,
  "sort": "submitTime,desc"
}
```

## 응답 샘플
```json
{
  "items": [
    {
      "msgId": 1,
      "subject": "SMS 배송 안내",
      "message": "배송이 시작되었습니다. 운송장 확인 부탁드립니다.",
      "msgType": "1",
      "msgTypeNm": "SMS",
      "status": "2",
      "statusNm": "전송완료",
      "scheduleTime": "20250105103000",
      "submitTime": "20250105103010",
      "checkDate": "20250105",
      "callbackNum": "01000000000",
      "rcptData": "01000000000",
      "result": "SUCCESS",
      "resultDesc": "Delivered",
      "externalMessageId": "EXT-2025-0001",
      "requestedAt": "20250105102930",
      "sentAt": "20250105103015",
      "errorCode": null,
      "retryCount": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 33,
  "totalPages": 2,
  "sort": "submitTime: DESC"
}
```

## 참고
- 포트폴리오용으로 민감정보가 제거되어 있습니다.
- DB는 H2 인메모리로 동작하며 샘플 데이터가 포함됩니다.
- 기본 샘플 계정: `DEMO_USER` / `password`

## Swagger UI
- http://localhost:8080/swagger-ui.html

## H2 콘솔
- http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:oms`
- User: `sa`
- Password: (빈 값)

## API 요약
- `POST /api/auth/register` : 사용자 등록
- `POST /api/auth/token` : JWT 발급
- `POST /api/messages/search` : 메시지 이력 조회
- `GET /api/health` : 헬스체크
