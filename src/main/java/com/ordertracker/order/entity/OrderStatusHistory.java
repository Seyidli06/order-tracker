package com.ordertracker.order.entity;

import com.ordertracker.common.enums.OrderStatus;
import com.ordertracker.common.enums.StatusChangeSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_order_status_history_order"
            )
    )
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "previous_status",
            length = 50
    )
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "new_status",
            nullable = false,
            length = 50
    )
    private OrderStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private StatusChangeSource source;

    @Column(
            name = "reference_id",
            length = 255
    )
    private String referenceId;

    @Column(
            name = "changed_at",
            nullable = false,
            updatable = false
    )
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}