package nayoung.inventorymanagementsystem.web.order.model;

public class RequestOrder {
    public Long itemId;
    public Long count;

    public RequestOrder(Long itemId, Long count) {
        this.itemId = itemId;
        this.count = count;
    }
}
