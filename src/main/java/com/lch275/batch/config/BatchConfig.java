package com.lch275.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 공통 설정
 * - JobRepository, PlatformTransactionManager 등 인프라 Bean 설정
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
}
