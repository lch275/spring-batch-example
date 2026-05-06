package com.lch275.batch.job.order.chunck;

import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import com.lch275.batch.job.order.domain.Order;
import com.lch275.batch.job.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * 주문 데이터를 DB에 저장하는 Writer
 */
@Slf4j
@RequiredArgsConstructor
public class OrderLoadItemWriter implements ItemWriter<OrderDTO> {

    private final OrderRepository orderRepository;

    @Override
    public void write(Chunk<? extends OrderDTO> chunk) {
        List<Order> orders = chunk.getItems().stream()
                .map(Order::from)
                .toList();
        orderRepository.saveAll(orders);
        log.info("Saved {} orders to DB", orders.size());
    }
}
