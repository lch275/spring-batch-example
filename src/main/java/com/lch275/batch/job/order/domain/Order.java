package com.lch275.batch.job.order.domain;

import com.lch275.batch.job.order.chunck.dto.OrderDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String productName;
    private int quantity;
    private double price;
    private String orderDate;

    public static Order from(OrderDTO dto) {
        Order order = new Order();
        order.orderId = dto.getOrderId();
        order.productName = dto.getProductName();
        order.quantity = dto.getQuantity();
        order.price = dto.getPrice();
        order.orderDate = dto.getOrderDate();
        return order;
    }
}
