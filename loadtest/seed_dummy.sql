\set ON_ERROR_STOP on
\if :{?user_count}
\else
\set user_count 100
\endif
\echo [seed] expected_user_count=:user_count

-- ---------------------------------------------------------------------------
-- Q-Feed load-test seed (current schema aligned)
--
-- Prerequisites:
-- 1) Generate token CSV first:
--    node loadtest/generate_user_tokens.mjs --user-count 100 --start-user-id 1 --out-dir loadtest/generated
-- 2) Run this SQL on Docker:
--    docker compose -f loadtest/docker-compose.yaml up -d postgres
--    docker cp loadtest/seed_dummy.sql qfeed-loadtest-postgres:/tmp/seed_dummy.sql
--    docker cp loadtest/generated/user-token-list.csv qfeed-loadtest-postgres:/tmp/user-token-list.csv
--    docker exec -it qfeed-loadtest-postgres psql -h localhost -p 5432 -U postgres -d qfeed -v user_count=100 -f /tmp/seed_dummy.sql
--    psql -h localhost -p 5432 -U postgres -d qfeed -v user_count=100 -f loadtest/seed_dummy.sql
--
-- Seed scope:
-- - users: :user_count (token csv row count must match)
-- - per user history:
--   - PRACTICE_INTERVIEW 10
--   - REAL_INTERVIEW 10
-- - question pool:
--   - CS + SYSTEM_DESIGN only (PORTFOLIO excluded)
--   - 10 questions per (type, category)
-- - auth:
--   - insert refresh-token compatible data into user_oauth_rtr_family/user_oauth_refresh
--   - token source: loadtest/generated/user-token-list.csv
-- ---------------------------------------------------------------------------

BEGIN;

TRUNCATE TABLE
    public.interview_session_metric,
    public.interview_topic_feedback,
    public.interview_turn,
    public.interview_session_feedback,
    public.interview_session,
    public.answer_metric,
    public.answer_hashtag,
    public.answer,
    public.question_hashtag,
    public.question,
    public.metric,
    public.hashtag,
    public.user_oauth_refresh,
    public.user_oauth_rtr_family,
    public.user_oauth,
    public.user_account
RESTART IDENTITY CASCADE;

CREATE TEMP TABLE tmp_auth_token_seed (
    user_id bigint NOT NULL,
    nickname text NOT NULL,
    family_uuid varchar(36) NOT NULL,
    access_token text NOT NULL,
    refresh_token text NOT NULL,
    refresh_token_hash varchar(64) NOT NULL,
    issued_at timestamp without time zone NOT NULL,
    access_expires_at timestamp without time zone NOT NULL,
    refresh_expires_at timestamp without time zone NOT NULL
) ON COMMIT DROP;

\copy tmp_auth_token_seed (user_id, nickname, family_uuid, access_token, refresh_token, refresh_token_hash, issued_at, access_expires_at, refresh_expires_at) FROM '/tmp/user-token-list.csv' WITH (FORMAT csv, HEADER true)

CREATE TEMP TABLE tmp_seed_params (
    expected_user_count int NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_seed_params (expected_user_count)
VALUES (:user_count);

DO $$
DECLARE
    v_count int;
    v_expected int;
BEGIN
    SELECT count(*) INTO v_count FROM tmp_auth_token_seed;
    SELECT expected_user_count INTO v_expected FROM tmp_seed_params;
    IF v_count <> v_expected THEN
        RAISE EXCEPTION 'Expected % token rows, got %', v_expected, v_count;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 1) Core master data
-- ---------------------------------------------------------------------------

-- metric: fixed 5
INSERT INTO public.metric (use_yn, created_at, updated_at, metric_nm, metric_desc)
VALUES
    (true, now(), now(), '논리력', '답변의 논리적 흐름과 구조'),
    (true, now(), now(), '완성도', '질문의 핵심 이해도'),
    (true, now(), now(), '전달력', '명확하고 간결한 전달'),
    (true, now(), now(), '정확도', '개념 정확성'),
    (true, now(), now(), '구체성', '실무 맥락의 구체적 설명');

-- optional hashtags for richer immediate-feedback fixtures
INSERT INTO public.hashtag (use_yn, created_at, updated_at, tag_nm, tag_desc)
SELECT
    true,
    now(),
    now(),
    'seed_tag_' || g,
    'seed hashtag ' || g
FROM generate_series(1, 130) AS g;

-- users from token seed
INSERT INTO public.user_account (
    account_id,
    account_email,
    account_status_cd,
    account_nick_nm,
    file_id,
    account_last_login_at,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    t.user_id,
    format('lt_user_%s@qfeed.dev', lpad(t.user_id::text, 3, '0')),
    'ACTIVE',
    t.nickname,
    NULL,
    now(),
    now() - ((t.user_id % 30) || ' days')::interval,
    now(),
    NULL
FROM tmp_auth_token_seed t
ORDER BY t.user_id;

-- auth family + refresh token storage (current RTR model)
INSERT INTO public.user_oauth_rtr_family (
    family_uuid,
    account_id,
    family_device_info,
    family_client_ip,
    family_last_used_at,
    family_expires_at,
    family_revoked,
    family_revoked_at,
    family_revoked_reason,
    created_at,
    updated_at
)
SELECT
    t.family_uuid,
    t.user_id,
    format('k6-seed-device-%s', t.user_id),
    '127.0.0.1',
    now(),
    t.refresh_expires_at,
    false,
    NULL,
    NULL,
    now(),
    now()
FROM tmp_auth_token_seed t;

INSERT INTO public.user_oauth_refresh (
    family_id,
    token_hash,
    token_expires_at,
    token_used,
    token_used_at,
    created_at,
    updated_at
)
SELECT
    f.family_id,
    t.refresh_token_hash,
    t.refresh_expires_at,
    false,
    NULL,
    now(),
    now()
FROM tmp_auth_token_seed t
JOIN public.user_oauth_rtr_family f
    ON f.family_uuid = t.family_uuid;

-- normalize legacy question category check constraint (for older dumps)
DO $$
DECLARE
    v_constraint_name text;
BEGIN
    FOR v_constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE c.contype = 'c'
          AND n.nspname = 'public'
          AND t.relname = 'question'
          AND pg_get_constraintdef(c.oid) ILIKE '%question_ctg%'
    LOOP
        EXECUTE format(
                'ALTER TABLE public.question DROP CONSTRAINT IF EXISTS %I',
                v_constraint_name
                );
    END LOOP;
END $$;

ALTER TABLE public.question
    ADD CONSTRAINT question_question_ctg_check
        CHECK (
            (question_ctg)::text = ANY ((
                ARRAY[
                    'OS',
                    'NETWORK',
                    'DB',
                    'COMPUTER_ARCHITECTURE',
                    'DATA_STRUCTURE_ALGORITHM',
                    'SOCIAL',
                    'NOTIFICATION',
                    'REALTIME',
                    'SEARCH',
                    'MEDIA',
                    'STORAGE',
                    'PLATFORM',
                    'TRANSACTION',
                    'PORTFOLIO'
                    ]
                )::text[])
            );

-- question pool: CS + SYSTEM_DESIGN only, 10 per (type, category)
WITH question_seed(question_type_cd, question_ctg, question_list) AS (
    VALUES
        (
            'CS',
            'OS',
            ARRAY[
                '프로세스와 스레드의 차이를 설명하고 각각의 장단점을 비교해보세요.',
                '컨텍스트 스위칭이 발생하는 시점과 비용을 줄이는 방법을 설명해보세요.',
                '데드락의 4가지 필요 조건과 실무에서의 대응 전략을 설명해보세요.',
                '가상 메모리와 페이지 폴트의 동작 과정을 단계별로 설명해보세요.',
                '뮤텍스와 세마포어의 차이를 사례와 함께 설명해보세요.',
                '시스템 콜이 사용자 모드에서 커널 모드로 전환되는 과정을 설명해보세요.',
                'CPU 스케줄링에서 선점형과 비선점형의 차이와 적용 사례를 설명해보세요.',
                '파일 디스크립터와 I/O 멀티플렉싱 개념을 설명해보세요.',
                '인터럽트와 트랩의 차이와 커널에서 처리되는 흐름을 설명해보세요.',
                '스레드 안전성을 보장하기 위한 동기화 방법을 예시와 함께 설명해보세요.'
            ]::text[]
        ),
        (
            'CS',
            'NETWORK',
            ARRAY[
                'TCP 3-way handshake와 4-way handshake의 목적을 설명해보세요.',
                'HTTP/1.1, HTTP/2, HTTP/3의 차이와 성능 특성을 비교해보세요.',
                'TCP와 UDP를 어떤 기준으로 선택하는지 실제 사례로 설명해보세요.',
                'TLS 핸드셰이크 과정과 인증서 검증 흐름을 설명해보세요.',
                'L4 로드밸런서와 L7 로드밸런서의 차이와 선택 기준을 설명해보세요.',
                'DNS 조회 과정에서 Recursive Resolver와 Authoritative DNS 역할을 설명해보세요.',
                'NAT와 PAT의 동작 원리와 한계를 설명해보세요.',
                '네트워크 혼잡 제어에서 slow start와 congestion avoidance를 설명해보세요.',
                'Keep-Alive와 커넥션 풀링이 지연시간에 미치는 영향을 설명해보세요.',
                'WebSocket과 SSE의 차이와 적용 시나리오를 비교해보세요.'
            ]::text[]
        ),
        (
            'CS',
            'DB',
            ARRAY[
                'B-Tree 인덱스가 동작하는 방식과 인덱스가 불리한 경우를 설명해보세요.',
                'Nested Loop Join, Hash Join, Sort Merge Join의 차이와 사용 조건을 설명해보세요.',
                '트랜잭션의 ACID 특성을 실무 예시와 함께 설명해보세요.',
                '격리 수준에 따른 Dirty Read, Non-repeatable Read, Phantom Read 차이를 설명해보세요.',
                '정규화와 비정규화의 트레이드오프를 설명해보세요.',
                '샤딩과 파티셔닝의 차이와 각각이 유리한 상황을 설명해보세요.',
                '읽기 복제(Replication) 구성에서 일관성 이슈를 어떻게 다루는지 설명해보세요.',
                'RDBMS와 NoSQL을 선택할 때 고려할 기준을 설명해보세요.',
                'DB 락 경합과 데드락을 줄이기 위한 설계 방법을 설명해보세요.',
                'Materialized View와 일반 View의 차이와 활용 사례를 설명해보세요.'
            ]::text[]
        ),
        (
            'CS',
            'COMPUTER_ARCHITECTURE',
            ARRAY[
                'CPU 캐시(L1/L2/L3) 구조와 캐시 미스가 성능에 미치는 영향을 설명해보세요.',
                '파이프라이닝의 개념과 Hazard(데이터/제어/구조) 해결 방식을 설명해보세요.',
                'TLB가 무엇이며 주소 변환 성능에 어떤 영향을 주는지 설명해보세요.',
                '메모리 계층 구조와 지역성(Locality) 원리를 설명해보세요.',
                'x86과 ARM 아키텍처의 차이와 서버 환경에서의 선택 기준을 설명해보세요.',
                '브랜치 예측이 CPU 성능에 미치는 영향과 실패 시 비용을 설명해보세요.',
                'SIMD가 무엇이며 어떤 워크로드에서 성능 향상이 큰지 설명해보세요.',
                '멀티코어 환경에서 False Sharing 문제가 생기는 원인을 설명해보세요.',
                'NUMA 아키텍처에서 메모리 접근 최적화 전략을 설명해보세요.',
                'DMA 동작 원리와 CPU 오프로드 효과를 설명해보세요.'
            ]::text[]
        ),
        (
            'CS',
            'DATA_STRUCTURE_ALGORITHM',
            ARRAY[
                '배열과 연결 리스트의 시간복잡도 차이를 연산별로 설명해보세요.',
                '스택과 큐의 동작 원리와 사용 사례를 설명해보세요.',
                '해시테이블 충돌 해결 방식(체이닝, 오픈어드레싱)을 비교해보세요.',
                '트리 순회(전위/중위/후위/레벨순회) 방법과 활용 사례를 설명해보세요.',
                '이진 탐색이 동작하기 위한 전제 조건과 구현 시 주의점을 설명해보세요.',
                'DFS와 BFS의 차이와 각각 적합한 문제 유형을 설명해보세요.',
                '다익스트라 알고리즘의 동작 원리와 한계를 설명해보세요.',
                '동적 계획법(DP)의 핵심 아이디어와 적용 절차를 설명해보세요.',
                '퀵정렬, 병합정렬, 힙정렬의 평균/최악 시간복잡도를 비교해보세요.',
                'LRU 캐시를 구현할 때 적합한 자료구조 조합을 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'SOCIAL',
            ARRAY[
                '대규모 소셜 서비스에서 뉴스피드 생성 방식을 fan-out on write와 fan-out on read로 비교해보세요.',
                '팔로우 그래프를 저장할 때 읽기와 쓰기 성능을 균형 있게 맞추는 방법을 설명해보세요.',
                '좋아요 수와 댓글 수를 실시간으로 집계할 때 일관성과 성능을 어떻게 맞출지 설명해보세요.',
                '인기 게시물 랭킹 시스템을 설계할 때 시간 가중치 반영 방법을 설명해보세요.',
                '이미지와 영상이 포함된 게시물 업로드 파이프라인을 설계해보세요.',
                '개인화 추천 피드를 제공할 때 온라인 추론과 배치 계산을 어떻게 분리할지 설명해보세요.',
                '신고와 차단 기능이 피드 노출에 반영되는 아키텍처를 설계해보세요.',
                '팔로워가 많은 계정의 트래픽 핫스팟을 완화하는 전략을 설명해보세요.',
                '실시간 알림과 피드 동기화를 함께 제공할 때 데이터 모델을 설명해보세요.',
                '소셜 서비스 장애 시 데이터 복구와 재처리 전략을 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'NOTIFICATION',
            ARRAY[
                '대량 푸시 알림 발송 시스템에서 큐 기반 아키텍처를 설계해보세요.',
                '알림 중복 발송을 방지하기 위한 idempotency 전략을 설명해보세요.',
                '알림 발송 rate limit 정책을 사용자 단위와 글로벌 단위로 설계해보세요.',
                '사용자 알림 선호 설정(채널, 시간대)을 반영하는 구조를 설명해보세요.',
                '푸시 실패 시 SMS나 이메일로 대체 발송하는 fallback 전략을 설명해보세요.',
                '알림 템플릿 렌더링 서비스를 다국어로 운영하는 방법을 설명해보세요.',
                '재시도와 Dead Letter Queue를 이용해 실패 알림을 관리하는 방법을 설명해보세요.',
                '알림 도달률과 열람률을 추적하는 이벤트 수집 구조를 설명해보세요.',
                '멀티 리전 환경에서 알림 지연을 최소화하는 라우팅 전략을 설명해보세요.',
                '마케팅 캠페인 예약 발송 기능을 설계할 때 고려할 사항을 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'REALTIME',
            ARRAY[
                '대규모 채팅 시스템에서 메시지 전달 보장 전략을 설계해보세요.',
                '온라인 상태(Presence) 시스템을 저지연으로 제공하는 방법을 설명해보세요.',
                '메시지 순서 보장을 위해 서버와 클라이언트에서 필요한 처리를 설명해보세요.',
                '채팅방 단위 샤딩 전략과 리밸런싱 전략을 설명해보세요.',
                'WebSocket 서버 확장 시 세션 고정과 로드밸런싱 전략을 설명해보세요.',
                '실시간 서비스에서 백프레셔를 적용하는 방법을 설명해보세요.',
                '모바일 네트워크 환경에서 재접속과 세션 복구 전략을 설명해보세요.',
                '타이핑 인디케이터와 읽음 상태를 확장 가능하게 처리하는 방법을 설명해보세요.',
                '실시간 음성/영상 통신에서 시그널링 서버 역할을 설명해보세요.',
                '일시 접속 끊김 이후 누락 메시지 재전송 구조를 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'SEARCH',
            ARRAY[
                '검색 시스템에서 역색인(Inverted Index) 생성과 조회 흐름을 설명해보세요.',
                '신규 문서가 유입될 때 인덱싱 파이프라인을 실시간에 가깝게 구성하는 방법을 설명해보세요.',
                '검색 결과 랭킹(관련도, 최신성, 인기)을 결합하는 방식을 설명해보세요.',
                '자동완성 기능을 Trie와 n-gram 관점에서 설계해보세요.',
                '필터/정렬/집계를 제공하는 Faceted Search 구조를 설명해보세요.',
                '오탈자 교정과 형태소 분석을 포함한 쿼리 전처리 전략을 설명해보세요.',
                '검색 캐시를 적용할 때 캐시 키와 만료 전략을 설명해보세요.',
                '무중단 Reindex를 위한 인덱스 버전 전환 방식을 설명해보세요.',
                '권한 기반 검색 결과 필터링을 효율적으로 처리하는 방법을 설명해보세요.',
                '검색 장애 시 fallback 응답 전략과 관측 지표를 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'MEDIA',
            ARRAY[
                '대용량 영상 업로드 후 트랜스코딩 파이프라인을 설계해보세요.',
                '적응형 비트레이트(ABR) 스트리밍 제공 구조를 설명해보세요.',
                'CDN 캐시 히트율을 높이기 위한 URL 설계와 캐시 정책을 설명해보세요.',
                '썸네일 추출과 미리보기 생성 워크플로를 설계해보세요.',
                '라이브 스트리밍에서 지연시간을 줄이기 위한 전략을 설명해보세요.',
                '미디어 파일 스토리지 비용 절감을 위한 계층화 정책을 설명해보세요.',
                '콘텐츠 저작권 보호를 위한 워터마킹/DRM 적용 구조를 설명해보세요.',
                '콘텐츠 검수(모더레이션) 파이프라인을 자동화하는 방법을 설명해보세요.',
                '트래픽 스파이크 시 미디어 서비스 확장 전략을 설명해보세요.',
                '미디어 재생 이벤트 분석 파이프라인을 설계해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'STORAGE',
            ARRAY[
                '객체 스토리지 서비스에서 메타데이터 저장 구조를 설계해보세요.',
                '대용량 파일 청크 업로드와 재개(Resume) 기능을 설계해보세요.',
                '중복 제거(Deduplication)와 압축 전략의 트레이드오프를 설명해보세요.',
                '파일 버전 관리와 충돌 해결 정책을 설계해보세요.',
                '공유 링크 접근 권한 만료와 추적 기능을 설계해보세요.',
                '강한 일관성과 최종 일관성 중 어떤 모델을 선택할지 설명해보세요.',
                '멀티 리전 복제를 적용할 때 RPO/RTO 목표를 맞추는 방법을 설명해보세요.',
                '저장 데이터 암호화와 키 관리 체계를 설계해보세요.',
                '삭제된 파일 복구와 Garbage Collection 정책을 설계해보세요.',
                '파일 접근 이력 감사 로그를 확장 가능하게 저장하는 방법을 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'PLATFORM',
            ARRAY[
                '마이크로서비스 환경에서 서비스 디스커버리 구조를 설명해보세요.',
                'API Gateway에서 인증/인가/레이트리밋을 통합 처리하는 방법을 설명해보세요.',
                '설정 변경(Config) 롤아웃을 안전하게 배포하는 전략을 설명해보세요.',
                '관측 가능성(로그/메트릭/트레이싱) 표준화를 설계해보세요.',
                '장애 전파를 막기 위한 Circuit Breaker와 Bulkhead 패턴을 설명해보세요.',
                '오토스케일링 기준 지표와 스케일 정책을 설계해보세요.',
                'Blue-Green과 Canary 배포 전략의 차이와 선택 기준을 설명해보세요.',
                '시크릿 관리와 키 로테이션 자동화 체계를 설계해보세요.',
                '멀티테넌트 환경에서 테넌트 격리 전략을 설명해보세요.',
                '대규모 장애 대응을 위한 런북과 자동 복구 절차를 설명해보세요.'
            ]::text[]
        ),
        (
            'SYSTEM_DESIGN',
            'TRANSACTION',
            ARRAY[
                '결제 승인과 캡처를 분리한 트랜잭션 흐름을 설계해보세요.',
                '중복 결제를 방지하기 위한 idempotency key 설계를 설명해보세요.',
                '분산 트랜잭션에서 Saga 패턴으로 보상 처리를 설계해보세요.',
                '복식부기 원장을 기반으로 정산 시스템을 설계해보세요.',
                '결제 데이터와 정산 데이터 불일치 시 재조정(Reconciliation) 프로세스를 설명해보세요.',
                '부정 거래 탐지를 실시간으로 적용하는 파이프라인을 설계해보세요.',
                '정산 배치 지연이 발생할 때 사용자 노출 전략을 설명해보세요.',
                '환불과 취소가 동시에 발생할 때 상태 전이 모델을 설계해보세요.',
                'Outbox 패턴을 사용한 이벤트 발행 신뢰성 보장 방법을 설명해보세요.',
                '금융 규제 대응을 위한 감사 추적(Audit Trail) 저장 전략을 설명해보세요.'
            ]::text[]
        )
),
expanded AS (
    SELECT
        s.question_type_cd,
        s.question_ctg,
        u.question_content,
        u.ord
    FROM question_seed s
    CROSS JOIN LATERAL unnest(s.question_list) WITH ORDINALITY AS u(question_content, ord)
)
INSERT INTO public.question (
    use_yn,
    created_at,
    updated_at,
    deleted_at,
    question_type_cd,
    question_ctg,
    question_content
)
SELECT
    true,
    now() - (e.ord || ' days')::interval,
    now(),
    NULL,
    e.question_type_cd,
    e.question_ctg,
    e.question_content
FROM expanded e
ORDER BY e.question_type_cd, e.question_ctg, e.ord;

-- minimal question_hashtag mapping (2 hashtags per question)
WITH q AS (
    SELECT question_id, row_number() OVER (ORDER BY question_id) AS rn
    FROM public.question
),
expanded AS (
    SELECT
        q.question_id,
        ((q.rn * 2 - 1) % 130) + 1 AS tag_id_1,
        ((q.rn * 2) % 130) + 1 AS tag_id_2
    FROM q
)
INSERT INTO public.question_hashtag (
    question_id,
    tag_id,
    created_at,
    updated_at
)
SELECT question_id, tag_id_1, now(), now() FROM expanded
UNION ALL
SELECT question_id, tag_id_2, now(), now() FROM expanded;

CREATE TEMP TABLE tmp_question_pool AS
SELECT
    q.question_id,
    q.question_type_cd,
    q.question_ctg,
    q.question_content,
    row_number() OVER (ORDER BY q.question_id) AS ord
FROM public.question q
WHERE q.use_yn = true
  AND q.deleted_at IS NULL
  AND q.question_type_cd IN ('CS', 'SYSTEM_DESIGN')
ORDER BY q.question_id;

-- ---------------------------------------------------------------------------
-- 2) PRACTICE_INTERVIEW x 10 per user
-- ---------------------------------------------------------------------------

CREATE TEMP TABLE tmp_practice_seed AS
SELECT
    u.account_id AS user_id,
    s.seq_no,
    format('seed-practice-u%s-s%s', lpad(u.account_id::text, 3, '0'), lpad(s.seq_no::text, 2, '0')) AS session_id,
    q.question_id,
    q.question_type_cd,
    q.question_ctg,
    q.question_content,
    now() - make_interval(days => (u.account_id % 20)::int, mins => (s.seq_no * 6)::int) AS started_at,
    format('연습모드 답변(user=%s, seq=%s): %s 핵심을 설명합니다.', u.account_id, s.seq_no, q.question_ctg) AS answer_text
FROM public.user_account u
CROSS JOIN generate_series(1, 10) AS s(seq_no)
JOIN tmp_question_pool q
  ON q.ord = (((u.account_id - 1) * 10 + s.seq_no - 1) % (SELECT count(*) FROM tmp_question_pool)) + 1
ORDER BY u.account_id, s.seq_no;

INSERT INTO public.interview_session (
    session_id,
    account_id,
    interview_type,
    question_type_cd,
    session_status_cd,
    initial_question_id,
    session_started_at,
    session_ended_at,
    session_expires_at,
    created_at,
    updated_at
)
SELECT
    p.session_id,
    p.user_id,
    'PRACTICE_INTERVIEW',
    p.question_type_cd,
    'COMPLETED',
    p.question_id,
    p.started_at,
    p.started_at + interval '1 minute',
    p.started_at + interval '1 day',
    p.started_at,
    p.started_at + interval '1 minute'
FROM tmp_practice_seed p;

INSERT INTO public.interview_turn (
    session_id,
    turn_order,
    topic_id,
    turn_type_cd,
    question_ctg,
    question_text,
    answer_text,
    created_at,
    updated_at
)
SELECT
    p.session_id,
    0,
    1,
    'new_topic',
    p.question_ctg,
    p.question_content,
    p.answer_text,
    p.started_at,
    p.started_at + interval '20 seconds'
FROM tmp_practice_seed p;

INSERT INTO public.interview_session_feedback (
    session_id,
    bad_case_type_cd,
    bad_case_message,
    bad_case_guidance,
    keyword_covered_json,
    keyword_missing_json,
    keyword_coverage_ratio,
    overall_strengths_feedback,
    overall_improvements_feedback,
    created_at,
    updated_at
)
SELECT
    p.session_id,
    NULL,
    NULL,
    NULL,
    jsonb_build_array('핵심 개념', '구조적 설명'),
    jsonb_build_array('실무 사례'),
    0.7,
    format('연습모드 강점(user=%s, seq=%s): 핵심을 정확히 답변했습니다.', p.user_id, p.seq_no),
    format('연습모드 개선점(user=%s, seq=%s): 예시를 1개 추가하면 더 좋습니다.', p.user_id, p.seq_no),
    p.started_at + interval '40 seconds',
    p.started_at + interval '50 seconds'
FROM tmp_practice_seed p;

INSERT INTO public.interview_topic_feedback (
    session_id,
    topic_id,
    main_question_text,
    strengths_feedback,
    improvements_feedback,
    created_at,
    updated_at
)
SELECT
    p.session_id,
    1,
    p.question_content,
    '핵심 개념 전달이 명확했습니다.',
    '트레이드오프 관점 설명을 보완해보세요.',
    p.started_at + interval '45 seconds',
    p.started_at + interval '55 seconds'
FROM tmp_practice_seed p;

INSERT INTO public.interview_session_metric (
    session_id,
    metric_id,
    metric_score,
    metric_comment,
    created_at,
    updated_at
)
SELECT
    p.session_id,
    m.metric_id,
    3 + ((p.user_id + p.seq_no + m.metric_id) % 3),
    format('practice metric(user=%s, seq=%s, metric=%s)', p.user_id, p.seq_no, m.metric_id),
    p.started_at + interval '45 seconds',
    p.started_at + interval '55 seconds'
FROM tmp_practice_seed p
CROSS JOIN public.metric m;

INSERT INTO public.answer (
    account_id,
    question_id,
    answer_content,
    answer_status_cd,
    answer_type,
    answer_session_id,
    answer_ai_feedback,
    answer_strengths_feedback,
    answer_improvements_feedback,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    p.user_id,
    p.question_id,
    p.answer_text,
    'COMPLETED',
    'PRACTICE_INTERVIEW',
    p.session_id,
    format('연습모드 종합 피드백(user=%s, seq=%s)', p.user_id, p.seq_no),
    '핵심 전달이 좋았습니다.',
    '구체적인 사례를 추가해보세요.',
    p.started_at + interval '1 minute',
    p.started_at + interval '1 minute',
    NULL
FROM tmp_practice_seed p;

-- ---------------------------------------------------------------------------
-- 3) REAL_INTERVIEW x 10 per user
-- ---------------------------------------------------------------------------

CREATE TEMP TABLE tmp_real_seed AS
SELECT
    u.account_id AS user_id,
    s.seq_no,
    format('seed-real-u%s-s%s', lpad(u.account_id::text, 3, '0'), lpad(s.seq_no::text, 2, '0')) AS session_id,
    q.question_id,
    q.question_type_cd,
    q.question_ctg,
    q.question_content,
    now() - make_interval(days => (u.account_id % 25)::int, mins => (s.seq_no * 9 + 200)::int) AS started_at
FROM public.user_account u
CROSS JOIN generate_series(1, 10) AS s(seq_no)
JOIN tmp_question_pool q
  ON q.ord = (((u.account_id - 1) * 10 + s.seq_no + 37) % (SELECT count(*) FROM tmp_question_pool)) + 1
ORDER BY u.account_id, s.seq_no;

INSERT INTO public.interview_session (
    session_id,
    account_id,
    interview_type,
    question_type_cd,
    session_status_cd,
    initial_question_id,
    session_started_at,
    session_ended_at,
    session_expires_at,
    created_at,
    updated_at
)
SELECT
    r.session_id,
    r.user_id,
    'REAL_INTERVIEW',
    r.question_type_cd,
    'COMPLETED',
    r.question_id,
    r.started_at,
    r.started_at + interval '12 minute',
    r.started_at + interval '90 minute',
    r.started_at,
    r.started_at + interval '12 minute'
FROM tmp_real_seed r;

INSERT INTO public.interview_turn (
    session_id,
    turn_order,
    topic_id,
    turn_type_cd,
    question_ctg,
    question_text,
    answer_text,
    created_at,
    updated_at
)
SELECT
    r.session_id,
    0,
    1,
    'new_topic',
    r.question_ctg,
    r.question_content,
    format('실전모드 답변(user=%s, seq=%s) - 메인 질문 답변', r.user_id, r.seq_no),
    r.started_at,
    r.started_at + interval '2 minute'
FROM tmp_real_seed r
UNION ALL
SELECT
    r.session_id,
    1,
    1,
    'follow_up',
    r.question_ctg,
    format('꼬리질문 1 (%s)', r.question_ctg),
    format('실전모드 답변(user=%s, seq=%s) - 꼬리질문 1 답변', r.user_id, r.seq_no),
    r.started_at + interval '3 minute',
    r.started_at + interval '6 minute'
FROM tmp_real_seed r
UNION ALL
SELECT
    r.session_id,
    2,
    2,
    'new_topic',
    r.question_ctg,
    format('토픽 전환 질문 (%s)', r.question_ctg),
    format('실전모드 답변(user=%s, seq=%s) - 토픽 전환 답변', r.user_id, r.seq_no),
    r.started_at + interval '7 minute',
    r.started_at + interval '10 minute'
FROM tmp_real_seed r;

INSERT INTO public.interview_session_feedback (
    session_id,
    bad_case_type_cd,
    bad_case_message,
    bad_case_guidance,
    keyword_covered_json,
    keyword_missing_json,
    keyword_coverage_ratio,
    overall_strengths_feedback,
    overall_improvements_feedback,
    created_at,
    updated_at
)
SELECT
    r.session_id,
    NULL,
    NULL,
    NULL,
    jsonb_build_array('핵심 개념', '구조화', '정확도'),
    jsonb_build_array('트레이드오프'),
    0.8,
    format('실전모드 강점(user=%s, seq=%s): 구조화와 정확도가 우수합니다.', r.user_id, r.seq_no),
    format('실전모드 개선점(user=%s, seq=%s): 트레이드오프와 엣지케이스를 보강하세요.', r.user_id, r.seq_no),
    r.started_at + interval '11 minute',
    r.started_at + interval '12 minute'
FROM tmp_real_seed r;

INSERT INTO public.interview_topic_feedback (
    session_id,
    topic_id,
    main_question_text,
    strengths_feedback,
    improvements_feedback,
    created_at,
    updated_at
)
SELECT
    r.session_id,
    1,
    r.question_content,
    '핵심 개념 설명이 명확합니다.',
    '근거 수치 예시를 보강해보세요.',
    r.started_at + interval '10 minute',
    r.started_at + interval '11 minute'
FROM tmp_real_seed r
UNION ALL
SELECT
    r.session_id,
    2,
    format('토픽 전환 질문 (%s)', r.question_ctg),
    '이전 답변 맥락을 잘 이어갔습니다.',
    '간결한 결론 요약을 추가해보세요.',
    r.started_at + interval '11 minute',
    r.started_at + interval '12 minute'
FROM tmp_real_seed r;

INSERT INTO public.interview_session_metric (
    session_id,
    metric_id,
    metric_score,
    metric_comment,
    created_at,
    updated_at
)
SELECT
    r.session_id,
    m.metric_id,
    4 + ((r.user_id + r.seq_no + m.metric_id) % 2),
    format('real metric(user=%s, seq=%s, metric=%s)', r.user_id, r.seq_no, m.metric_id),
    r.started_at + interval '10 minute',
    r.started_at + interval '12 minute'
FROM tmp_real_seed r
CROSS JOIN public.metric m;

INSERT INTO public.answer (
    account_id,
    question_id,
    answer_content,
    answer_status_cd,
    answer_type,
    answer_session_id,
    answer_ai_feedback,
    answer_strengths_feedback,
    answer_improvements_feedback,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    r.user_id,
    r.question_id,
    format('실전모드 최종 답변(user=%s, seq=%s)', r.user_id, r.seq_no),
    'COMPLETED',
    'REAL_INTERVIEW',
    r.session_id,
    format('실전모드 종합 피드백(user=%s, seq=%s)', r.user_id, r.seq_no),
    '논리적 구조가 우수합니다.',
    '핵심 결론을 한 문장으로 요약해보세요.',
    r.started_at + interval '12 minute',
    r.started_at + interval '12 minute',
    NULL
FROM tmp_real_seed r;

-- ---------------------------------------------------------------------------
-- 4) Answer metrics + immediate-feedback fixtures
-- ---------------------------------------------------------------------------

INSERT INTO public.answer_metric (
    answer_id,
    metric_id,
    answer_metric_score,
    answer_metric_comment,
    created_at,
    updated_at
)
SELECT
    a.answer_id,
    m.metric_id,
    CASE
        WHEN a.answer_type = 'PRACTICE_INTERVIEW'
            THEN 3 + ((a.answer_id + m.metric_id) % 3)
        ELSE 4 + ((a.answer_id + m.metric_id) % 2)
    END AS score,
    format('answer metric(answer=%s, metric=%s)', a.answer_id, m.metric_id),
    a.created_at,
    a.updated_at
FROM public.answer a
CROSS JOIN public.metric m;

-- 2 hashtags per answer (for immediate feedback list fixtures)
INSERT INTO public.answer_hashtag (
    answer_id,
    tag_id,
    keyword_included,
    created_at,
    updated_at
)
SELECT
    a.answer_id,
    ((a.answer_id * 2 - 1) % 130) + 1,
    true,
    a.created_at,
    a.updated_at
FROM public.answer a
UNION ALL
SELECT
    a.answer_id,
    ((a.answer_id * 2) % 130) + 1,
    false,
    a.created_at,
    a.updated_at
FROM public.answer a;

-- ---------------------------------------------------------------------------
-- 5) Sequence sync
-- ---------------------------------------------------------------------------

DO $$
DECLARE
    v_seq text;
BEGIN
    v_seq := pg_get_serial_sequence('public.user_account', 'account_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(account_id) FROM public.user_account), 1), true)',
            v_seq
        );
    END IF;

    v_seq := pg_get_serial_sequence('public.question', 'question_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(question_id) FROM public.question), 1), true)',
            v_seq
        );
    END IF;

    v_seq := pg_get_serial_sequence('public.answer', 'answer_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(answer_id) FROM public.answer), 1), true)',
            v_seq
        );
    END IF;

    v_seq := pg_get_serial_sequence('public.metric', 'metric_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(metric_id) FROM public.metric), 1), true)',
            v_seq
        );
    END IF;

    v_seq := pg_get_serial_sequence('public.hashtag', 'tag_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(tag_id) FROM public.hashtag), 1), true)',
            v_seq
        );
    END IF;

    v_seq := pg_get_serial_sequence('public.user_oauth_rtr_family', 'family_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(family_id) FROM public.user_oauth_rtr_family), 1), true)',
            v_seq
        );
    END IF;

    v_seq := pg_get_serial_sequence('public.user_oauth_refresh', 'token_id');
    IF v_seq IS NOT NULL THEN
        EXECUTE format(
            'SELECT setval(%L, COALESCE((SELECT max(token_id) FROM public.user_oauth_refresh), 1), true)',
            v_seq
        );
    END IF;
END $$;

COMMIT;

-- Quick summary
SELECT 'users' AS key, count(*) AS value FROM public.user_account
UNION ALL
SELECT 'questions', count(*) FROM public.question
UNION ALL
SELECT 'practice_answers', count(*) FROM public.answer WHERE answer_type = 'PRACTICE_INTERVIEW'
UNION ALL
SELECT 'real_answers', count(*) FROM public.answer WHERE answer_type = 'REAL_INTERVIEW'
UNION ALL
SELECT 'sessions_practice', count(*) FROM public.interview_session WHERE interview_type = 'PRACTICE_INTERVIEW'
UNION ALL
SELECT 'sessions_real', count(*) FROM public.interview_session WHERE interview_type = 'REAL_INTERVIEW'
UNION ALL
SELECT 'refresh_tokens', count(*) FROM public.user_oauth_refresh;
