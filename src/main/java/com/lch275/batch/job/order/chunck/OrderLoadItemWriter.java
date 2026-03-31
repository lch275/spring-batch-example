package com.lch275.batch.job.order.chunck;

import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * 주문 데이터 저장 Writer (현재는 로그 출력만)
 */
@Slf4j
public class OrderLoadItemWriter implements ItemWriter<OrderDTO> {

    @Override
    public void write(Chunk<? extends OrderDTO> chunk) {
        chunk.getItems().forEach(order -> log.info("Written order: {}", order));
    }
}
