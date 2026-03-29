package com.lch275.batch.common.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 모든 Job에 공통 적용 가능한 로깅 Listener
 */
@Slf4j
@Component
public class JobLoggerListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job 시작 - name: {}, params: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Job 종료 - name: {}, status: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus());
    }
}
