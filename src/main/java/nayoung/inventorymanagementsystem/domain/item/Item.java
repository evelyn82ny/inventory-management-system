package nayoung.inventorymanagementsystem.domain.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long quantity;

    protected Item(Long quantity) {
        this.quantity = quantity;
    }

    public Boolean decreaseQuantity(Long quantity) {
        if (this.quantity < quantity) return false;

        this.quantity -= quantity;
        return true;
    }

    public Long getQuantity() {
        return this.quantity;
    }
}
