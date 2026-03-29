# Spring Batch 예시 프로젝트

Spring Batch의 핵심 개념과 실무 패턴을 학습하기 위한 예시 프로젝트입니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.13
- Spring Batch 5
- Spring Data JPA
- MySQL
- Gradle

---

## 프로젝트 구조

```
src/main/java/com/lch275/batch/
├── BatchApplication.java
├── config/                           # 공통 설정
│   └── BatchConfig.java
├── common/                           # 공통 컴포넌트
│   ├── listener/
│   │   └── JobLoggerListener.java
│   └── util/
├── domain/                           # JPA Entity
├── repository/                       # JPA Repository
└── job/
    └── example/                      # Job별 패키지
        ├── ExampleJobConfig.java
        ├── chunk/
        │   ├── ExampleItemReader.java
        │   ├── ExampleItemProcessor.java
        │   ├── ExampleItemWriter.java
        │   └── dto/
        │       ├── ExampleInput.java
        │       └── ExampleOutput.java
        └── tasklet/
            └── ExampleTasklet.java
```

---

## 주요 설계 원칙

### 1. Job별 패키지 분리 (Feature-based Package)

각 Job은 `job/{jobName}/` 아래 독립적인 패키지를 가집니다.

```
job/
├── example/        # ExampleJob 관련 클래스 전체
├── settlement/     # SettlementJob 관련 클래스 전체
└── cleanup/        # CleanupJob 관련 클래스 전체
```

역할(Reader/Processor/Writer)별로 분리하는 타입 기반 구조 대신, Job 단위로 묶어 **응집도를 높이고 변경 범위를 최소화**합니다. 특정 Job을 수정하거나 삭제할 때 해당 패키지만 건드리면 됩니다.

---

### 2. Chunk 지향 처리와 Tasklet의 역할 구분

Spring Batch의 Step은 두 가지 방식으로 구현합니다.

| 방식 | 사용 시점 | 예시 |
|------|-----------|------|
| **Chunk** | 대량 데이터를 읽고 → 가공 → 저장하는 반복 처리 | DB 데이터 집계, 파일 변환 |
| **Tasklet** | 단순하고 일회성인 작업 | 파일 삭제, 상태값 업데이트, 알림 발송 |

하나의 Job 안에서 두 방식을 혼합할 수 있습니다. 예시 Job의 경우 Tasklet(전처리) → Chunk(본 처리) 순으로 Step을 구성합니다.

---

### 3. Chunk 내부 역할 분리 (Reader / Processor / Writer)

```
chunk/
├── ExampleItemReader.java      # 데이터 소스에서 읽기 (DB, File, API)
├── ExampleItemProcessor.java   # 비즈니스 로직 (변환, 필터링, 검증)
├── ExampleItemWriter.java      # 처리 결과 저장 (DB, File, API)
└── dto/
    ├── ExampleInput.java       # Reader 출력 / Processor 입력 타입
    └── ExampleOutput.java      # Processor 출력 / Writer 입력 타입
```

- **Reader**: 오직 읽기에만 집중합니다. 비즈니스 로직을 포함하지 않습니다.
- **Processor**: `null`을 반환하면 해당 아이템이 Writer로 전달되지 않아 필터링에 활용합니다.
- **Writer**: Chunk 단위(묶음)로 한 번에 저장하여 I/O를 최소화합니다.
- **DTO 분리**: Reader → Processor 사이의 `Input` 타입과 Processor → Writer 사이의 `Output` 타입을 분리해 각 단계의 결합도를 낮춥니다.

---

### 4. 공통 컴포넌트는 common 패키지로 분리

여러 Job에서 재사용하는 컴포넌트는 `common/` 아래에 둡니다.

```
common/
├── listener/   # JobExecutionListener, StepExecutionListener
└── util/       # 날짜 변환, 파일 처리 등 공통 유틸
```

Job별 패키지에는 해당 Job에만 사용되는 클래스만 위치시킵니다. 두 개 이상의 Job에서 공유되는 순간 `common/`으로 이동합니다.

---

### 5. Job 설정은 JobConfig 한 곳에서 관리

`ExampleJobConfig.java` 하나에 Job, Step 빈 선언을 모두 모읍니다.

```java
@Bean
public Job exampleJob() { ... }

@Bean
public Step exampleTaskletStep() { ... }

@Bean
public Step exampleChunkStep() { ... }
```

Step의 흐름(순서, 조건 분기, 재시도 정책)을 한 파일에서 파악할 수 있어 가독성이 높아집니다.

---

## 새 Job 추가 방법

1. `job/{newJobName}/` 패키지 생성
2. `{NewJobName}JobConfig.java` 작성 (Job, Step 빈 등록)
3. Chunk 방식이면 `chunk/` 하위에 Reader, Processor, Writer, dto 작성
4. Tasklet 방식이면 `tasklet/` 하위에 Tasklet 구현체 작성
5. 공통으로 쓰이는 컴포넌트가 생기면 `common/`으로 이동

# # Spring Batch 실무 학습 체크리스트

> 각 항목을 직접 구현하며 학습한다. 단순 동작 확인이 아니라, 메타테이블 상태 변화와 트랜잭션 경계를 함께 확인할 것.

---

## Phase 1: 기본기 — Job / Step / Chunk 패턴 체득

### 1-1. CSV → DB 적재 배치
- [ ] `FlatFileItemReader`로 주문 CSV 파일(order_id, product_name, quantity, price, order_date)을 읽는다.
- [ ] `ItemProcessor`에서 total_amount = quantity × price 를 계산하여 필드를 추가한다.
- [ ] `JdbcBatchItemWriter`로 orders 테이블에 bulk insert 한다.
- [ ] 파일에 잘못된 행(컬럼 수 불일치)이 섞여 있을 때 예외가 발생하는 것을 확인한다.

### 1-2. DB → CSV 추출 배치
- [ ] `JdbcPagingItemReader`로 orders 테이블에서 특정 날짜 범위의 데이터를 조회한다.
- [ ] `FlatFileItemWriter`로 CSV 파일을 생성하며, 헤더 라인을 포함시킨다.
- [ ] 출력 파일명에 실행일자를 포함시킨다 (예: `orders_20260329.csv`).

### 1-3. JobParameter 활용
- [ ] `targetDate`를 JobParameter로 받아서 Reader의 WHERE 조건에 바인딩한다.
- [ ] 같은 targetDate로 두 번 실행하면 `JobInstanceAlreadyCompleteException`이 발생하는 것을 확인한다.
- [ ] `RunIdIncrementer`를 적용한 후 동일 파라미터로 재실행이 가능해지는 것을 확인한다.

### 1-4. 메타테이블 동작 이해
- [ ] Job 실행 후 `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION` 테이블을 직접 조회한다.
- [ ] COMPLETED 상태의 Job과 FAILED 상태의 Job에서 각 테이블 데이터 차이를 비교한다.
- [ ] FAILED 상태의 Job을 재실행하면 새로운 JOB_EXECUTION이 생기는 것을 확인한다.

### 1-5. 멀티 Step Job 구성
- [ ] Step 1: Tasklet으로 staging 테이블을 truncate 한다.
- [ ] Step 2: Chunk 방식으로 원본 테이블 → staging 테이블로 데이터를 이관한다.
- [ ] Step 3: Tasklet으로 처리 완료 로그를 남긴다.
- [ ] Step 2가 실패하면 Step 3은 실행되지 않는 것을 확인한다.

---

## Phase 2: 파일 처리 심화

### 2-1. 고정길이 전문 파싱
- [ ] 금융권 전문 형식의 고정길이 파일을 준비한다 (예: 기관코드 4자리 + 계좌번호 14자리 + 금액 15자리 + 거래일 8자리).
- [ ] `FixedLengthTokenizer`로 각 필드를 위치 기반으로 파싱한다.
- [ ] 헤더 1행(전체 건수, 총금액)과 푸터 1행(검증 합계)은 `LinesToSkip` 또는 커스텀 `LineCallbackHandler`로 처리한다.
- [ ] 푸터의 합계와 실제 처리 금액이 불일치하면 Step을 FAILED 처리한다.

### 2-2. 멀티 파일 순차 처리
- [ ] 지정된 디렉토리에서 `*.csv` 파일을 모두 읽어야 한다.
- [ ] `MultiResourceItemReader`를 사용하여 파일 단위로 순차 처리한다.
- [ ] 각 파일별 처리 건수를 `StepExecutionListener`에서 로깅한다.

### 2-3. 파일 아카이빙
- [ ] 처리 완료된 파일을 `archive/yyyyMMdd/` 디렉토리로 이동시키는 Tasklet을 구현한다.
- [ ] 이동 대상 파일 목록은 이전 Step의 ExecutionContext에서 전달받는다.
- [ ] 이동 실패 시(디스크 용량 부족 등) Step을 FAILED 처리하고, 이미 이동된 파일 목록을 로깅한다.

### 2-4. 파일 검증 Step
- [ ] chunk 처리 전에 검증 Step을 둔다.
- [ ] 검증 항목: 파일 존재 여부, 라인 수 최소 기준 충족 여부, 헤더 포맷 일치 여부.
- [ ] 검증 실패 시 Job을 즉시 중단하고, 검증 성공 시에만 다음 chunk Step으로 진행한다 (`on("FAILED").end()` / `on("*").to(chunkStep)` 조건 분기).

### 2-5. 대용량 파일 chunk size 튜닝
- [ ] 100만 행 CSV 파일을 준비한다.
- [ ] chunk size를 100 / 500 / 1000 / 5000으로 바꿔가며 처리 시간을 측정한다.
- [ ] `StepExecution`의 readCount, writeCount, commitCount, rollbackCount를 확인한다.

---

## Phase 3: 통계 / 집계 / 회계 배치

### 3-1. 일별 거래 집계
- [ ] 거래 테이블(transaction)에서 특정 일자의 데이터를 읽는다.
- [ ] `ItemProcessor`에서 카테고리별로 건수와 금액 합계를 집계한다 (인메모리 Map 활용).
- [ ] `ItemWriter` 또는 `StepExecutionListener.afterStep()`에서 집계 결과를 daily_summary 테이블에 insert 한다.
- [ ] 동일 일자를 재처리할 경우 기존 집계를 삭제 후 재적재하는 멱등성을 보장한다.

### 3-2. 수수료 / 세금 계산 Processor
- [ ] 거래 유형별로 다른 수수료율을 적용하는 `ItemProcessor`를 구현한다.
    - 예: 카드 결제 → 2.5%, 계좌이체 → 0.3%, 해외 송금 → 1.2% + 고정 수수료 5,000원.
- [ ] 부가세(10%)를 별도 필드로 계산하여 함께 저장한다.
- [ ] 수수료 계산 결과가 음수가 되는 비정상 케이스는 skip 처리하고 오류 테이블에 기록한다.

### 3-3. Step 간 집계 데이터 전달 → 리포트 생성
- [ ] Step 1: 거래 데이터를 집계하고, 총건수/총금액/카테고리별 합계를 `ExecutionContext`에 저장한다.
- [ ] Step 2: `ExecutionContext`에서 집계 데이터를 꺼내어 리포트용 CSV 파일을 생성한다.
- [ ] `ExecutionContextPromotionListener`를 사용하여 Step → Job 레벨로 데이터를 승격시킨다.

### 3-4. 월말 마감 배치
- [ ] Step 1 (전월 데이터 확정): 해당 월의 거래 상태를 'CONFIRMED'로 일괄 업데이트한다.
- [ ] Step 2 (집계): 확정된 거래 데이터로 월별 집계 테이블을 생성한다.
- [ ] Step 3 (원장 반영): 집계 결과를 원장(ledger) 테이블에 반영하고, 잔액을 갱신한다.
- [ ] Step 1 ~ 3 중 어느 단계에서 실패해도 해당 Step부터 재시작(restart) 가능하도록 구성한다.

### 3-5. 대사(reconciliation) 처리
- [ ] chunk 처리 완료 후 `StepExecutionListener.afterStep()`에서 처리 건수와 금액 합계를 검증한다.
- [ ] 원본 데이터의 건수/합계와 처리 결과의 건수/합계가 불일치하면 Step을 FAILED로 마킹한다.
- [ ] 불일치 상세 내역(차이 건수, 차이 금액)을 로그와 별도 테이블에 기록한다.

---

## Phase 4: 외부 시스템 연동 — API 호출

### 4-1. API 호출로 데이터 보강
- [ ] DB에서 읽은 고객 데이터에 대해, 외부 신용평가 API를 호출하여 신용등급을 조회한다.
- [ ] `ItemProcessor` 내에서 `RestTemplate` 또는 `WebClient`를 사용한다.
- [ ] API 응답의 신용등급을 원본 데이터에 추가하여 Writer에 전달한다.
- [ ] API 타임아웃을 3초로 설정하고, 타임아웃 발생 시 동작을 확인한다.

### 4-2. Retry 정책 적용
- [ ] API 호출 시 `HttpServerErrorException`(5xx)이 발생하면 최대 3회 재시도한다.
- [ ] 재시도 간격은 exponential backoff로 설정한다 (1초 → 2초 → 4초).
- [ ] `@Retryable` 또는 Spring Batch의 `faultTolerant().retry()` 방식 중 택 1로 구현한다.
- [ ] 3회 재시도 후에도 실패하면 해당 건을 skip 처리한다.

### 4-3. Skip + Retry 조합
- [ ] 4xx 응답(클라이언트 오류)은 재시도 없이 즉시 skip → 오류 테이블에 기록한다.
- [ ] 5xx 응답(서버 오류)은 3회 retry 후 실패 시 skip → 오류 테이블에 기록한다.
- [ ] `ConnectException`(연결 불가)은 retry 없이 Step 자체를 FAILED 처리한다.
- [ ] skip된 건수가 전체의 10%를 초과하면 Step을 FAILED 처리하는 로직을 추가한다.

### 4-4. API 응답 캐싱
- [ ] 동일한 고객 ID로 중복 API 호출이 발생하지 않도록 인메모리 캐시를 적용한다.
- [ ] 캐시는 `ConcurrentHashMap` 또는 Spring Cache(`@Cacheable`)를 활용한다.
- [ ] 캐시 히트율을 Step 종료 시 로깅한다.

### 4-5. API Rate Limit 대응
- [ ] 외부 API의 rate limit이 초당 10건이라고 가정한다.
- [ ] `ChunkListener.afterChunk()`에서 chunk 처리 후 남은 quota를 계산하여 필요 시 sleep 한다.
- [ ] 또는 `RateLimiter`(Guava/Resilience4j)를 Processor에 적용한다.
- [ ] rate limit 초과로 429 응답을 받으면 retry-after 헤더만큼 대기 후 재시도한다.

---

## Phase 5: 외부 시스템 연동 — Kafka 이벤트 발행

### 5-1. 처리 완료 이벤트 발행
- [ ] chunk 처리 완료된 데이터를 `ItemWriter`에서 Kafka topic(`order-completed`)으로 발행한다.
- [ ] 메시지 포맷: JSON (`{"orderId": "...", "status": "COMPLETED", "processedAt": "..."}`).
- [ ] `KafkaTemplate`을 Writer 내부에서 사용한다.
- [ ] 발행 성공/실패 여부를 `ListenableFutureCallback`으로 확인하고 로깅한다.

### 5-2. 트랜잭션 정합성 처리
- [ ] DB 쓰기와 Kafka 발행이 하나의 chunk 트랜잭션에 묶일 때, Kafka 발행 실패 시 DB도 rollback 되는지 확인한다.
- [ ] `ChainedKafkaTransactionManager` 또는 `ChainedTransactionManager`를 적용하여 DB + Kafka 트랜잭션을 묶는다.
- [ ] 트랜잭션 분리가 필요한 경우: DB 커밋 후 Kafka 발행 실패 시 보상 로직을 설계한다.

### 5-3. Kafka 발행 Step 분리
- [ ] Step 1: DB에서 읽어 staging 테이블에 적재한다 (DB 트랜잭션만).
- [ ] Step 2: staging 테이블에서 읽어 Kafka로 발행한다 (Kafka 트랜잭션만).
- [ ] Step 2 실패 시 Step 2부터 재시작하면 미발행 건만 처리되도록 상태 플래그를 관리한다.

### 5-4. 발행 실패 보상 배치
- [ ] Kafka 발행 실패 건을 `failed_events` 테이블에 저장한다.
- [ ] 별도 보상 배치 Job을 만들어 `failed_events` 테이블을 읽고 재발행한다.
- [ ] 재발행도 실패하면 retry_count를 증가시키고, 3회 초과 시 DEAD_LETTER 상태로 마킹한다.
- [ ] DEAD_LETTER 건은 알림을 발송하여 수동 처리할 수 있도록 한다.

### 5-5. Kafka Consumer 배치 (역방향)
- [ ] `KafkaItemReader`로 특정 topic에서 메시지를 consume 하여 DB에 적재하는 Job을 구현한다.
- [ ] 배치 실행 시 마지막 offset부터 읽도록 offset 관리 방식을 결정한다.
- [ ] 메시지 역직렬화 실패 시 해당 메시지를 skip 하고 DLT(Dead Letter Topic)로 전송한다.

---

## Phase 6: 운영 안정성

### 6-1. Skip 정책 + 오류 기록
- [ ] `faultTolerant().skipLimit(100).skip(DataFormatException.class)`를 설정한다.
- [ ] `SkipListener.onSkipInProcess()`에서 skip된 원본 데이터와 예외 메시지를 error_log 테이블에 insert 한다.
- [ ] skip 건수가 임계치(예: 100건)에 도달하면 Step이 자동 FAILED 되는 것을 확인한다.

### 6-2. 실패 Job 재시작
- [ ] 1000건 처리 중 500건째에서 의도적으로 예외를 발생시켜 Job을 FAILED 상태로 만든다.
- [ ] 동일 JobParameter로 재실행하면 501건째부터 처리가 재개되는 것을 확인한다.
- [ ] `BATCH_STEP_EXECUTION` 테이블의 `READ_COUNT`, `COMMIT_COUNT`가 누적되는 것을 확인한다.

### 6-3. 실행 알림 연동
- [ ] `JobExecutionListener.afterJob()`에서 Job 종료 상태에 따라 알림을 발송한다.
- [ ] 성공 시: 처리 건수/소요 시간을 Slack 또는 Telegram으로 발송한다.
- [ ] 실패 시: 실패 Step 이름, 예외 메시지, 마지막 처리 건을 포함하여 발송한다.

### 6-4. 배치 실행 이력 조회 API
- [ ] `JobExplorer`를 주입받아 최근 Job 실행 목록을 조회하는 REST API를 만든다.
- [ ] 응답에 jobName, status, startTime, endTime, exitDescription을 포함한다.
- [ ] 특정 JobExecution의 Step별 상세 정보(readCount, writeCount, skipCount)를 조회하는 API도 추가한다.

### 6-5. 스케줄링 + 동시 실행 방지
- [ ] `@Scheduled(cron = "0 0 2 * * *")`으로 매일 새벽 2시에 Job을 실행한다.
- [ ] `JobLauncher`에 `SyncTaskExecutor`를 설정하여 동기 실행되도록 한다.
- [ ] 이미 실행 중인 동일 Job이 있으면 중복 실행을 방지하는 로직을 구현한다 (JobExplorer로 STARTED 상태 확인).

---

## Phase 7: 성능 — 대용량 처리

### 7-1. Partitioning 병렬 처리
- [ ] `Partitioner`를 구현하여 ID 범위 기반으로 데이터를 N개 파티션으로 분할한다 (예: 1~10000, 10001~20000, ...).
- [ ] `TaskExecutorPartitionHandler`에 스레드 풀 크기 4를 설정한다.
- [ ] 전체 처리 시간이 단일 스레드 대비 단축되는 것을 측정한다.
- [ ] 각 파티션의 처리 건수 합계가 전체 데이터 건수와 일치하는 것을 검증한다.

### 7-2. AsyncItemProcessor + AsyncItemWriter
- [ ] 기존 동기 Processor/Writer를 `AsyncItemProcessor`, `AsyncItemWriter`로 감싼다.
- [ ] 스레드 풀 크기를 조절하며 처리 시간을 비교한다.
- [ ] 비동기 처리 시 예외가 발생하면 어느 시점에 감지되는지 확인한다.

### 7-3. Reader 성능 비교
- [ ] 동일 데이터(50만 건)에 대해 `JdbcCursorItemReader` vs `JdbcPagingItemReader` 처리 시간을 측정한다.
- [ ] CursorItemReader 사용 시 DB 커넥션이 Step 전체 동안 유지되는 것을 확인한다 (커넥션 풀 모니터링).
- [ ] PagingItemReader의 pageSize와 chunk size 관계에 따른 쿼리 호출 횟수를 확인한다.

### 7-4. Chunk Size 벤치마크
- [ ] 100만 건 데이터에 대해 chunk size 100 / 500 / 1000 / 5000으로 처리한다.
- [ ] 각 설정별 총 소요 시간, commit 횟수, DB 부하(쿼리 수)를 기록한다.
- [ ] 최적 chunk size를 결정하고, 그 근거를 문서화한다.

---

## 완료 기준

| 구분 | 목표 |
|------|------|
| Phase 1~2 | Spring Batch 기본 구조와 파일 처리를 자유롭게 구성할 수 있다 |
| Phase 3 | 통계/집계/회계 도메인의 배치를 설계하고 멱등성을 보장할 수 있다 |
| Phase 4~5 | 외부 API 호출과 Kafka 연동 시 장애 대응 전략을 수립할 수 있다 |
| Phase 6 | 운영 환경에서 배치를 모니터링하고 장애 복구할 수 있다 |
| Phase 7 | 대용량 데이터 처리 시 성능 병목을 진단하고 튜닝할 수 있다 |