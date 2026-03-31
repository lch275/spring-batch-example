package com.lch275.batch.job.order.chunck.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * CSV 한 줄에 대응하는 주문 DTO
 * 필드 순서: order_id, product_name, quantity, price, order_date
 */
@Getter
@Setter
@ToString
public class OrderDTO {

    private String orderId;
    private String productName;
    private int quantity;
    private double price;
    private String orderDate;
}
