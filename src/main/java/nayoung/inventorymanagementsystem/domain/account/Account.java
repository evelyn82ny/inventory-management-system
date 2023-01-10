package nayoung.inventorymanagementsystem.domain.account;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long balance = 0L;

    private Long numberOfOrders = 0L;

    protected Long increaseBalance(Long balance) {
        return this.balance += balance;
    }

    protected Long decreaseBalance(Long balance) {
        return this.balance -= balance;
    }

    protected Long getBalance() {
        return this.balance;
    }

    public void increaseNumberOfOrders() {
        this.numberOfOrders++;
    }
}
