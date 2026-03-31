package com.lch275.batch.job.order.chunck;

import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * 주문 데이터 가공 Processor (현재는 pass-through)
 */
@Slf4j
public class OrderLoadItemProcessor implements ItemProcessor<OrderDTO, OrderDTO> {

    @Override
    public OrderDTO process(OrderDTO item) {
        log.debug("Processing order: {}", item);
        return item;
    }
}
