package com.lch275.batch.job.order.chunck;

import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;

/**
 * 주문 CSV 파일을 읽는 FlatFileItemReader 팩토리
 * CSV 헤더: order_id, product_name, quantity, price, order_date
 */
public class OrderLoadItemReader {

    /**
     * @return FlatFileItemReader<OrderDTO>
     *
     * FlatFileItemReaderBuilder 주요 옵션
     * - name         : ExecutionContext 저장 키 (재시작 시 커서 위치 복원에 사용)
     * - resource     : 읽을 파일 경로
     * - linesToSkip  : 헤더 행 수 (건너뛸 줄)
     * - delimited()  : 구분자 기반 파싱 (기본값 쉼표)
     * - names        : CSV 컬럼 → DTO 필드 매핑 이름 (순서 일치 필수)
     * - targetType   : 매핑 대상 클래스 (BeanWrapperFieldSetMapper 자동 사용)
     */
    public static FlatFileItemReader<OrderDTO> create() {
        return new FlatFileItemReaderBuilder<OrderDTO>()
                .name("orderItemReader")
                .resource(new ClassPathResource("input/orders.csv"))
                .linesToSkip(1)                          // 헤더 1줄 건너뜀
                .delimited()
                    .delimiter(",")
                    .names("orderId", "productName", "quantity", "price", "orderDate")
                .targetType(OrderDTO.class)
                .build();
    }
}
