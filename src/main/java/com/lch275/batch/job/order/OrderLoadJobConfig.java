package com.lch275.batch.job.order;

import com.lch275.batch.job.order.chunck.OrderLoadItemProcessor;
import com.lch275.batch.job.order.chunck.OrderLoadItemReader;
import com.lch275.batch.job.order.chunck.OrderLoadItemWriter;
import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import com.lch275.batch.job.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * OrderLoadJob 설정
 * - Step 1 (Chunk): orders.csv → FlatFileItemReader → JPA 저장
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderLoadJobConfig {

    private static final int CHUNK_SIZE = 1000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final OrderRepository orderRepository;

    @Bean
    public Job orderLoadJob() {
        return new JobBuilder("orderLoadJob", jobRepository)
                .start(orderLoadStep())
                .build();
    }

    @Bean
    public Step orderLoadStep() {
        return new StepBuilder("orderLoadStep", jobRepository)
                .<OrderDTO, OrderDTO>chunk(CHUNK_SIZE, transactionManager)
                .reader(OrderLoadItemReader.create())       // FlatFileItemReader
                .processor(new OrderLoadItemProcessor())
                .writer(new OrderLoadItemWriter(orderRepository))
                .allowStartIfComplete(true)
                .build();
    }
}
