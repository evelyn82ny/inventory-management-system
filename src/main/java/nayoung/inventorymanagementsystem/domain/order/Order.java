package nayoung.inventorymanagementsystem.domain.order;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import nayoung.inventorymanagementsystem.domain.account.Account;
import nayoung.inventorymanagementsystem.domain.item.Item;

import javax.persistence.*;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Account account;

    @ManyToOne
    private Item item;

    private Long count;

    @Builder
    private Order(Account account, Item item, Long count) {
        this.account = account;
        this.item = item;
        this.count = count;
    }
}
