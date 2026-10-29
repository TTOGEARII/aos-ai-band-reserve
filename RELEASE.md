# 플레이스토어 첫 발매 체크리스트 — 합주실 예약 AI

> 앱: "합주실 예약 AI" (기기 아이콘 라벨 "합주실 예약"), 패키지 `com.enezzang.yatte`
> 백엔드: **기존 subculture-ground 백엔드를 그대로 사용** — `https://nest.kanenashitogeari.com` (이미 운영 중, Jenkins 자동 배포).
> 개발자 연락 이메일: cagameku3842@gmail.com (변경 가능)

---

## 0) 선행: 백엔드 연결 확인 (이미 운영 중 — 설정만)

- [x] 운영 API 확인: `https://nest.kanenashitogeari.com` 이 subculture-ground 백엔드를 서빙 (GET /events 200, /notion-agent/* 401 확인 — 2026-10-29)
- [x] 암호화 키 호환 확인: 운영 서버가 기본 `ENCRYPTION_KEY`를 사용함을 실제 로그인 호출로 검증 (복호화 성공 → 401 한국어 메시지)
- [x] 앱 `local.properties` 설정: `enezzangYatte.baseUrl=https://nest.kanenashitogeari.com` (키는 기본값이라 생략)
- [ ] 발매 전 재확인: 서버 `ENCRYPTION_KEY`를 나중에 커스텀 값으로 바꾸면 **앱도 `enezzangYatte.encryptionKey`를 맞춰 새 버전을 배포**해야 한다 (구버전 앱은 로그인부터 깨짐)

⏱ 예상: 완료됨. 막히기 쉬운 곳: 서버 키 변경 시 앱과의 동기화.

## 1) Google Play Console 개발자 계정 (개인)

- [ ] https://play.google.com/console 에서 개인 개발자 계정 등록 — 등록비 **$25(1회)**
- [ ] 신분증(정부 발급) 본인 확인 제출 → 승인 대기
- [ ] ⚠️ **개인 신규 계정은 프로덕션 공개 전 "비공개 테스트 요건"이 있다**: 일정 수의 테스터를 모집해 **14일 연속** 비공개 테스트를 통과해야 프로덕션 액세스를 신청할 수 있음. **정확한 테스터 인원수와 세부 조건은 콘솔 대시보드의 안내 문구를 기준으로 확인할 것** (수치는 변경될 수 있으므로 여기서 단정하지 않음)
- [ ] 콘솔 → 대시보드/프로덕션 트랙에서 내 계정에 이 요건이 적용되는지, 남은 조건이 무엇인지 확인

⏱ 예상: 등록 30분 + 신분증 검증 1~7일. 막히기 쉬운 곳: 신분증 검증 지연, 그리고 14일 테스트 요건을 모르고 프로덕션 일정 잡았다가 2주 밀리는 것.

## 2) 앱 서명

- [x] upload keystore **생성됨**: 프로젝트 루트 `release.keystore` (alias `enezzangyatte`, RSA 2048)
- [ ] keystore 파일과 비밀번호를 **서로 다른 2곳에 백업** (예: 1Password + 외장/클라우드) — 분실 시 업로드 키 재설정 절차가 번거로움
- [x] 프로젝트에 `keystore.properties` **작성됨**(git 제외) + `app/build.gradle.kts`에 release signingConfig 연결됨
- [ ] 콘솔에서 **Play App Signing** 사용 선택(기본값) — 앱 서명 키는 Google이 보관, 우리는 업로드 키만 관리

⏱ 예상: 남은 것은 백업 + 콘솔 설정. 막히기 쉬운 곳: keystore 비밀번호 백업을 미루는 것, `keystore.properties`를 실수로 커밋하는 것.

## 3) 릴리스 빌드

- [x] `local.properties`의 `enezzangYatte.baseUrl`이 **운영 주소**(`https://nest.kanenashitogeari.com`)로 설정됨 — 빌드 스크립트에 baseUrl 필수 가드가 있어 비어 있으면 실패함
- [x] 빌드 완료:
  ```bash
  ./gradlew :app:bundleRelease
  ```
- [x] 산출물 확인: `app/build/outputs/bundle/release/app-release.aab` → 서명된 발매 번들을 **`dist/enezzang-yatte-1.0-release.aab`로 보관** (운영 URL 포함)
- [ ] (권장) 릴리스 빌드를 실기기에 설치해 로그인→채팅→예약 링크까지 한 번 통과

⏱ 예상: 30분. 막히기 쉬운 곳: baseUrl이 로컬 IP(`http://<맥 IP>:3001`)로 남아 있는 채 빌드하는 것, 서명 설정 누락으로 unsigned aab가 나오는 것.

## 4) 콘솔에서 앱 만들기

- [ ] 앱 이름: **합주실 예약 AI**
- [ ] 기본 언어: **한국어(ko)**
- [ ] 앱/게임: **앱**
- [ ] 유료/무료: **무료** (무료→유료 전환은 나중에 불가하니 확정)

⏱ 예상: 10분. 막히기 쉬운 곳: 없음(단, 무료 선택은 되돌릴 수 없음).

## 5) 스토어 등록정보

- [ ] 등록 문구: `play/store-listing.md` **작성됨** — 제목/짧은 설명의 권장안 확정 후 콘솔에 입력 (한도: 짧은 설명 80자, 전체 설명 4000자)
- [ ] 앱 아이콘: `play/icon-512.png` **생성됨** (512×512 PNG, 불투명 배경 확인) — 콘솔 업로드
- [ ] 그래픽 이미지: `play/feature-1024x500.png` **생성됨** (1024×500 PNG) — 콘솔 업로드
- [x] 휴대전화 스크린샷 **3장 촬영됨** — `play/screenshots/{01-login,02-chat,03-settings}.png` (1080×1920, 9:16, 에뮬레이터·운영 서버 연동 상태로 촬영)
- [ ] (선택) 추가 촬영: Gemini 키를 넣고 실제 대화(합주실 검색 결과·예약 링크 버튼)가 보이는 화면 1~2장 — 전환율에 가장 효과적인 컷. 에뮬레이터 띄우고:
  ```bash
  adb shell wm size 1080x1920   # 9:16로 맞추고 촬영, 끝나면 wm size reset
  adb exec-out screencap -p > play/screenshots/04-conversation.png
  ```
- [ ] 콘솔에 전부 업로드 후 미리보기 확인

⏱ 예상: 30분(추가 촬영 시 +1시간). 막히기 쉬운 곳: 스크린샷 비율/해상도 요건 미달로 업로드 거부, 아이콘에 투명 배경 사용(거부됨).

## 6) 앱 콘텐츠 설문 (정책 > 앱 콘텐츠)

- [ ] **개인정보처리방침 URL**: https://nuxt.kanenashitogeari.com/bandroom-privacy.html — 파일은 subculture-ground-front `public/bandroom-privacy.html`에 **작성됨**. 시행일(`<YYYY-MM-DD>`) 채우고 커밋·배포 후 브라우저로 열리는지 확인
- [ ] 광고: **광고 없음**
- [ ] 콘텐츠 등급 설문(IARC): 채팅/AI 질문 주의 — **사용자 간 상호작용 아님**(채팅 상대는 AI뿐), **AI 생성 콘텐츠 있음**으로 답변
- [ ] 타겟층: **18세 이상 권장** (아동 대상 아님 — 아동 포함 선택 시 가족 정책 추가 요건 발생)
- [ ] 데이터 안전(Data safety) 양식 — **개인정보처리방침 내용과 일치해야 함**:
  - 수집: 이메일·이름(계정 관리 목적), 채팅 메시지(앱 기능 목적)
  - 사용자가 입력한 API 키(Gemini 필수 / Notion·YouTube 선택)는 서버 DB에 **암호화 저장**
  - 전송 중 암호화: 예 / 삭제 요청 가능: 예
  - 제3자 공유: 채팅 내용이 사용자 본인의 키로 **Google Gemini API**에 전달됨 + 도구 실행 시 네이버 예약·Notion·YouTube·악보 사이트 조회
  - 대화기록은 서버에 저장되지 않고 기기(DataStore)에만 보존 — 방침에도 동일하게 기재. 기기 백업 규칙(`backup_rules.xml`/`data_extraction_rules.xml`)이 DataStore를 클라우드 백업·기기 이전에서 제외하므로 "기기만" 서술이 그대로 성립
  - 위치정보·분석/트래킹 SDK·결제: 없음
  - 권한: **`INTERNET` 하나뿐** (`AndroidManifest.xml` 확인 — 위치·카메라·마이크·저장소·연락처 등 없음) — 양식의 권한 관련 답변도 이에 맞출 것

⏱ 예상: 1~2시간. 막히기 쉬운 곳: 데이터 안전 양식과 개인정보처리방침 내용이 어긋나면 심사 반려 — 두 문서를 같은 날 함께 작성할 것.

## 7) ⚠️ Google Play 생성형 AI 앱 정책

- [x] Google Play는 생성형 AI 앱에 **부적절한 AI 응답을 사용자가 신고할 수 있는 인앱 장치**를 요구함 — 요건 확인
- [x] 앱에 **구현됨**: 설정 화면의 **'부적절한 AI 응답 신고' 메일 링크**(mailto: cagameku3842@gmail.com, `ui/SettingsScreen.kt`) — 발매 번들(`dist/enezzang-yatte-1.0-release.aab`)에 포함됨
- [x] 개인정보처리방침(5장 "이용자의 권리와 행사 방법")에도 신고 수단 기재됨
- [ ] 신고 메일 주소를 바꾸면 **개인정보처리방침의 연락처와 같이** 바꿀 것 (`SettingsScreen.kt`의 `REPORT_EMAIL` 주석 참고)

⏱ 예상: 완료됨. 막히기 쉬운 곳: 메일 주소 변경 시 방침·스토어 연락처와 어긋나는 것.

## 8) 비공개 테스트 → 프로덕션

- [ ] 비공개 테스트 트랙에 `.aab` 업로드, 출시 노트(ko) 작성
- [ ] 테스터 모집: 이메일 목록 또는 Google 그룹으로 등록, 옵트인 링크 공유
- [ ] 테스터들이 실제로 설치·사용하도록 운영 (1단계 요건: **14일 연속** 테스트 유지 — 인원수 요건은 콘솔 안내 확인)
- [ ] 요건 충족 후 콘솔에서 **프로덕션 액세스 신청**(설문 답변) → 승인되면 프로덕션 트랙으로 승격 출시
- [ ] 첫 프로덕션 심사 통과 대기(보통 수일)

⏱ 예상: 최소 2주 + 심사 1~7일. 막히기 쉬운 곳: 테스터가 중간에 이탈해 14일 연속 조건이 깨지는 것 — 여유 인원으로 모집할 것.

## 9) 발매 후 운영

- [ ] 업데이트 때마다 `app/build.gradle.kts`의 **`versionCode` 증가**(현재 3) + `versionName` 갱신 — versionCode 동일하면 업로드 거부
- ℹ️ 콘솔의 **"네이티브 코드 디버그 기호 미업로드" 경고는 없앨 수 없다** — 네이티브는 androidx 프리빌트 .so 둘(graphics-path·DataStore)뿐인데 이미 stripped 상태로 배포돼 올릴 심볼이 존재하지 않는다(llvm-nm으로 확인, 2026-10-29). ndk.debugSymbolLevel=SYMBOL_TABLE 설정은 돼 있으므로 심볼 있는 의존성이 생기면 자동 포함된다. 경고는 출시·심사를 막지 않으니 무시할 것.
- [ ] 인앱 업데이트 도입 검토: water_fee처럼 **`play-app-update`**(In-App Update API) 적용해 강제/유연 업데이트 지원
- [ ] 크래시/ANR은 콘솔 Android Vitals에서 모니터링
- [ ] 백엔드 도메인(`nest.kanenashitogeari.com`) 변경·서버 이전 시 구버전 앱 호환 기간 고려

⏱ 예상: 상시. 막히기 쉬운 곳: versionCode 안 올리고 빌드해 업로드에서 막히는 것.
