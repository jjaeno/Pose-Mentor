# PoseMentor

> **AI 기반 운동 자세 분석 & 피드백 플랫폼**
> Java ✕ Spring Boot ✕ Swing

PoseMentor는 운동 영상을 손쉽게 업로드하여 AI로부터 자세 분석과 맞춤형 피드백을 즉시 받아볼 수 있는 프로젝트입니다.

---

## ✨ 주요 특징

| 영역             | 하이라이트                                        |
| -------------- | -------------------------------------------- |
| **다종목 지원**     | 골프 · 볼링 · 축구 · 야구 · 당구 · 농구 (확장 가능)          |
| **AI 자세 추정**   | **poserecognition API** 연동 → 관절 키포인트 JSON 획득 |
| **GPT 피드백 엔진** | 키포인트를 자연어 코칭 문장으로 변환 (gpt API)               |
| **데스크탑 클라이언트** | Java Swing 기반 GUI 제공                         |
| **모던 백엔드**     | Spring Boot 3.4 + Java 17 기반 REST API        |
| **비동기 처리**     | 로딩 중에도 UI가 멈추지 않도록 백그라운드 스레드 처리              |

---

## 🛠️ 기술 스택

| 계층           | 사용 기술                                                           |
| ------------ | --------------------------------------------------------------- |
| **Backend**  | Spring Boot 3.4 · Java 17 · Apache HttpClient · JavaCV · Lombok |
| **Frontend** | Java Swing GUI · Gradle (Application 플러그인)                      |

---

## 📂 폴더 구조

```
Pose-Mentor/
├─ posementor-backend/      # Spring Boot 서버
├─ posementor-frontend/     # Java Swing 클라이언트
└─ README.md
```

---

## 🚀 실행 방법

### 1. 필수 환경

* Java 17 이상
* Gradle 8 이상 (또는 `./gradlew` 사용)

### 2. 클론 및 빌드

```bash
$ git clone https://github.com/jjaeno/Pose-Mentor.git
$ cd Pose-Mentor

# 백엔드 실행
$ cd posementor-backend
$ ./gradlew bootRun

# 프론트엔드 실행
$ cd ../posementor-frontend
$ ./gradlew clean shadowJar
$ java -jar build/libs/posementor-frontend.jar
```

### 3. API 키 설정

> ⚠️ **주의:** PoseMentor는 외부 API 키 없이는 정상 동작하지 않습니다.
> 실행 전 반드시 아래 API 키를 발급받아 환경 설정에 입력해야 합니다:
>
> * 자세 인식 API: **poserecognition API** (예: RapidAPI 기반)
> * 피드백 생성 API: **gpt API** (예: OpenAI GPT)

환경 변수 예시 (`application-local.yml`):

```yaml
pose-api:
  url: https://your-poserecognition-api-url
  key: ${POSE_API_KEY}
openai:
  key: ${GPT_API_KEY}
```

실행 시 환경 변수로 주입:

```bash
$ POSE_API_KEY=your_pose_api_key GPT_API_KEY=your_gpt_key ./gradlew bootRun
```

`.env.example` 또는 `application-local-example.yml` 파일을 참고해 설정을 복사하고 `.gitignore`에 포함시키세요.

---


