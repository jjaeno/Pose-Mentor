# PoseMentor

> **AI 기반 운동 자세 분석 & 피드백 플랫폼**
> Java ✕ Spring Boot ✕ Swing

PoseMentor는 운동 영상을 손쉽게 업로드하여 AI로부터 자세 분석과 맞춤형 피드백을 즉시 받아볼 수 있는 프로젝트입니다.

---

## ✨ 주요 특징

| 영역                | 하이라이트                                                                |
| ----------------- | -------------------------------------------------------------------- |
| **다종목 지원**        | 골프 · 볼링 · 축구 · 야구 · 당구 · 농구 (추가 종목 손쉽게 확장)                           |
| **AI 자세 추정**      | **pose-recognition** API 연동 → 관절 키포인트 JSON 획득                  |
| **GPT 피드백 엔진**    | 키포인트를 자연어 코칭 문장으로 변환                                                 |
| **크로스 플랫폼 클라이언트** | *데스크탑*: Java Swing GUI · *모바일*: Android(Java) 앱(개발 중)                |
| **모던 백엔드**        | Spring Boot 3.4 + Java 17, Controller → Service → 외부 API → GPT 파이프라인 |
| **비동기 처리**        | GUI 로딩 중에도 백그라운드 스레드로 분석 진행                                          |

---

## 🖼️ 시스템 아키텍처

```
+-------------+      HTTP (mp4)      +-----------------+     HTTPS      +-------------+
|  Frontend   |  ─────────────────▶  |  PoseMentor API | ──────────────▶ | Pose API    |
| Swing /     |                      |  Spring Boot    |  keypoints     | (pose-recognition)  |
|                    JSON feedback   |  /api/analyze   | ◀────────────── |             |
+-------------+  ◀──────────────────  +-----------------+                +-------------+
                                          │
                                          │ GPT Prompt
                                          ▼
                                   +-----------------+
                                   |  OpenAI GPT     |
                                   +-----------------+
```

---

## 🛠️ 기술 스택

| 계층           | 사용 기술                                                                   |
| ------------ | ----------------------------------------------------------------------- |
| **Backend**  | Spring Boot 3.4 · Java 17 · Apache HttpClient 4.5 · JavaCV 1.5 · Lombok |
| **Frontend** | Java Swing(데스크탑) · Gradle(Application 플러그인)                             |
| **CI / Dev** | Gradle Wrapper · GitHub Actions(빌드 & 테스트)                               |

---

## 📂 폴더 구조

```
Pose-Mentor/
├─ posementor-backend/      # Spring Boot 백엔드
│  └─ src/main/java/com/posementor/
├─ posementor-frontend/     # Swing GUI (실행용 fat-jar 빌드)
│  └─ src/PoseMentorGUI.java
└─ README.md
```

---

## 🚀 시작 가이드

### 1. 사전 준비

* **Java 17+** 설치
* **Gradle 8+** (wrapper 포함)
* (선택) Android Studio Flamingo+ 모바일 클라이언트용

### 2. 클론 & 빌드

```bash
# 클론
$ git clone https://github.com/jjaeno/Pose-Mentor.git
$ cd Pose-Mentor

# 백엔드 실행
$ cd posementor-backend
$ ./gradlew bootRun           # http://localhost:8080

# 프론트엔드 실행(jar)
$ cd ../posementor-frontend
$ ./gradlew clean shadowJar   # posementor-frontend.jar 생성
$ java -jar build/libs/posementor-frontend.jar
```

---
