package com.lch275.batch.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 공통 설정
 * - JobRepository, PlatformTransactionManager 등 인프라 Bean 설정
 * - Spring Boot 3.x (Batch 5.x)에서는 @EnableBatchProcessing 불필요
 *   (사용 시 Boot Auto Configuration이 비활성화되어 메타 테이블 자동 생성 안 됨)
 */
@Configuration
public class BatchConfig {
}
