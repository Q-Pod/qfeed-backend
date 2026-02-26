# Load Test README

부하테스트 준비를 위한 데이터/토큰/가이드/샘플 스크립트를 모아둔 디렉터리입니다.

### 본 부하테스트 시나리오는 V2배포 및 DB 마이그레이션이 완료되었다고 가정하고 진행합니다~
## 1. 부하테스트 관련 변경사항 요약
- 전용 `loadtest/` 작업공간 추가
- 더미 데이터 적재 SQL(`seed_dummy.sql`) 추가
- JWT 기반 사용자 토큰 생성기(`generate_user_tokens.mjs`) 추가
- FE 호출 흐름 기준 가이드/명세 문서 추가
- k6 참고 스크립트(`test.js`) 추가
- 토큰 CSV 산출물(`generated/*.csv`) 관리
- 부하테스트를 위한 애플리케이션 설정 조정(`src/main/resources/application.yaml`)

## 2. 가이드 문서 요약
- `load-test-guide.md`: 클라우드팀 핸드오프 문서. 제공 범위, 데이터 기준, FE 기준 사이클(홈/연습/실전), 실행 순서 설명.
- `load-test-api-spec.md`: 부하테스트 대상 API 계약서. 엔드포인트별 목적, 요청/응답 형태, 최소 검증 포인트 정리.
- `policy-docs.md`: 현재 코드 기준 정책/제약(검증값, 인증/보안, 파일/S3, 인터뷰 세션 등) 정리 문서.

## 3. 클라우드팀 실행 플로우
아래는 프로젝트 루트(`17-JinyUs-Q-Feed-BE`) 기준입니다.

1. 원격 브랜치 이동

```bash
git fetch origin
git switch -c <branch-name> --track origin/<branch-name>
```

2. Docker DB 기동

```bash
docker compose -f loadtest/docker-compose.yaml up -d postgres
docker exec -it qfeed-loadtest-postgres pg_isready -U postgres -d qfeed
```

호스트에서 직접 접속할 때 Postgres 포트는 `5432`를 사용합니다.

3. 토큰 생성 (부하테스트 유저 수에 맞게 조정)

```bash
USER_COUNT=100
node loadtest/generate_user_tokens.mjs \
  --user-count "${USER_COUNT}" \
  --start-user-id 1 \
  --out-dir loadtest/generated
```

4. DB 시드 파일 컨테이너 반입

```bash
docker cp loadtest/seed_dummy.sql qfeed-loadtest-postgres:/tmp/seed_dummy.sql
docker cp loadtest/generated/user-token-list.csv qfeed-loadtest-postgres:/tmp/user-token-list.csv
```

5. 더미 데이터 적재

```bash
docker exec -it qfeed-loadtest-postgres \
  psql -h localhost -p 5432 -U postgres -d qfeed \
  -v user_count=100 \
  -f /tmp/seed_dummy.sql
```

6. 적재 확인

```bash
docker exec -it qfeed-loadtest-postgres psql -h localhost -p 5432 -U postgres -d qfeed -c "
SELECT 'users' AS key, count(*) AS value FROM public.user_account
UNION ALL
SELECT 'questions', count(*) FROM public.question
UNION ALL
SELECT 'practice_answers', count(*) FROM public.answer WHERE answer_type = 'PRACTICE_INTERVIEW'
UNION ALL
SELECT 'real_answers', count(*) FROM public.answer WHERE answer_type = 'REAL_INTERVIEW';
"
```

호스트에서 직접 확인하려면 아래처럼 `-p 5432`를 사용하세요.

```bash
psql -h localhost -p 5432 -U postgres -d qfeed -c "SELECT 1;"
```

7. 부하테스트 실행
- 권장: 클라우드팀 자체 시나리오 작성 (`load-test-guide.md`, `load-test-api-spec.md` 기준)
- 선택: 아래 k6 참고 스크립트 실행

```bash
k6 run loadtest/test.js \
  --env BASE_URL=http://{HOST}:8080 \
  --env USER_TOKEN_CSV=generated/user-access-token-list.csv \
  --env DURATION=5m
```

## 4. `test.js` 시나리오 설명

`loadtest/test.js`는 `constant-arrival-rate` 기반 2개 시나리오를 동시에 실행합니다.

- 테스트 시간 기본값: `DURATION=5m`
- `practice_cycle`: `PRACTICE_RATE=6` (초당 6 사이클 시작), `PRACTICE_PRE_ALLOCATED_VUS=20`, `PRACTICE_MAX_VUS=60`
- `practice_cycle` 흐름: 질문 조회 -> 연습 세션 생성 -> 연습 답변 제출 -> 최종 피드백 요청 -> 피드백 폴링
- `real_cycle`: `REAL_RATE=2` (초당 2 사이클 시작), `REAL_PRE_ALLOCATED_VUS=10`, `REAL_MAX_VUS=40`
- `real_cycle` 흐름: 실전 세션 생성 -> 실전 답변 턴 반복(최대 `REAL_TURN_LIMIT=8`) -> 최종 피드백 요청 -> 피드백 폴링

인증/토큰 처리:
- `USER_TOKEN_CSV`의 Access Token 사용
- 401 응답 시 `/api/auth/tokens`로 자동 갱신

필요 토큰 수(기본값):
- `practice maxVUs 60 + real maxVUs 40 = 최소 100명`

조정 가능한 주요 파라미터:
- `DURATION`
- `PRACTICE_RATE`, `PRACTICE_PRE_ALLOCATED_VUS`, `PRACTICE_MAX_VUS`
- `REAL_RATE`, `REAL_PRE_ALLOCATED_VUS`, `REAL_MAX_VUS`
- `FEEDBACK_POLL_MAX`, `FEEDBACK_POLL_INTERVAL_SEC`, `TOKEN_MARGIN_SEC`

## 5. 파일 설명
- `loadtest/seed_dummy.sql`: 부하테스트 더미 데이터 시드 SQL. 사용자/질문/답변/세션/피드백/RTR 토큰 데이터 생성.
- `loadtest/generate_user_tokens.mjs`: 사용자 수 기반 Access/Refresh 토큰 CSV 생성기.
- `loadtest/generated/user-token-list.csv`: DB 시드 입력용 토큰 CSV(`refresh_token_hash` 포함).
- `loadtest/generated/user-access-token-list.csv`: 시나리오 실행용 사용자 Access Token CSV.
- `loadtest/load-test-guide.md`: 실행 가이드 및 FE 호출 시퀀스 설명.
- `loadtest/load-test-api-spec.md`: 부하테스트 대상 API 명세.
- `loadtest/policy-docs.md`: 정책/제약 참조 문서.
- `loadtest/test.js`: k6 참고 시나리오(연습/실전 사이클) 스크립트.

## 6. 주의사항
- `loadtest/docker-compose.yaml` 기본 매핑 포트는 `5432 -> 5432`입니다.
- `postgres:18` 기준 데이터 볼륨 마운트는 `/var/lib/postgresql`를 사용합니다.
- `seed_dummy.sql`은 주요 테이블을 `TRUNCATE ... RESTART IDENTITY CASCADE`로 초기화합니다.
- `-v user_count` 값과 `user-token-list.csv`의 행 수가 다르면 시드가 실패합니다.
- 질문 카테고리 체크 제약(`question_question_ctg_check`)은 시드 중 자동 보정되도록 포함되어 있습니다.
- 토큰 기본 만료시간은 Access 6시간, Refresh 14일입니다.

### Postgres 18 볼륨 충돌 에러가 날 때
기존 잘못된 마운트(`/var/lib/postgresql/data`)로 생성된 볼륨이 남아있으면 아래처럼 초기화 후 다시 올리세요.

```bash
docker compose -f loadtest/docker-compose.yaml down -v
docker compose -f loadtest/docker-compose.yaml up -d postgres
docker exec -it qfeed-loadtest-postgres pg_isready -U postgres -d qfeed
```
