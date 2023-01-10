package nayoung.inventorymanagementsystem.domain.order;

import lombok.RequiredArgsConstructor;
import nayoung.inventorymanagementsystem.domain.account.Account;
import nayoung.inventorymanagementsystem.domain.account.AccountRepository;
import nayoung.inventorymanagementsystem.domain.item.Item;
import nayoung.inventorymanagementsystem.domain.item.ItemRepository;
import nayoung.inventorymanagementsystem.domain.redis.RedissonLockItem;
import nayoung.inventorymanagementsystem.web.order.model.RequestOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;
    private final RedissonLockItem redissonLockItem;

    @Transactional
    public void create(Long accountId, RequestOrder request) {
        Account account = accountRepository.findById(accountId).orElseThrow();

        if(!redissonLockItem.decreaseQuantity(request.itemId, request.count)) return;

        Item item = itemRepository.findById(request.itemId).orElseThrow();

        Order order = Order.builder()
                .account(account)
                .item(item)
                .count(request.count)
                .build();

        orderRepository.save(order);
        account.increaseNumberOfOrders();
    }
}
