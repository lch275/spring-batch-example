package com.lch275.batch.job.order;

import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import com.lch275.batch.job.order.domain.OrderRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderExportJobConfig {
    private static final int CHUNCK_SIZE = 10000;
    private final JobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager platformTransactionManager;
    private final OrderRepository orderRepository;

    @Bean
    public Job orderExportJob() {
        return new JobBuilder("orderExportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(orderExportStep())
                .build();
    }

    @Bean
    public Step orderExportStep() {
        return new StepBuilder("orderExportStep", jobRepository)
                .<OrderDTO, OrderDTO>chunk(CHUNCK_SIZE, platformTransactionManager)
                .reader(jpaPagingItemReader())
                .writer(flatFileItemWriter(null))
                .build();
    }

    @Bean
    public JpaPagingItemReader<OrderDTO> jpaPagingItemReader() {
        return new JpaPagingItemReaderBuilder<OrderDTO>()
                .name("jpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNCK_SIZE)
                .queryString("SELECT o FROM Order o")
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<OrderDTO> flatFileItemWriter(@Value("#{jobParameters['targetDate']}") String targetDate) {

        StringBuilder sb = new StringBuilder();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        sb.append("output/orders");
        sb.append("_");
        sb.append(targetDate);
        sb.append(".csv");

        return new FlatFileItemWriterBuilder<OrderDTO>()
                .name("flatFileItemWriter")
                .resource(new FileSystemResource(sb.toString()))
                .delimited()
                    .delimiter(",")
                    .names("orderId", "productName", "quantity", "price", "orderDate")
                .build();
    }
}
