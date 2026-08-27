package com.ordertracker.order.entity;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_orders_external_order_id",
                        columnNames = "external_order_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(
            nullable = false
    )
    private Long version;

    @Column(
            name = "external_order_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String externalOrderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user")
    )
    private User user;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private OrderStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (status == null) {
            status = OrderStatus.CREATED;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}