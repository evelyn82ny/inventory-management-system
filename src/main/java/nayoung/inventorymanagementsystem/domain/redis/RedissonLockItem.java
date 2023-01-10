package nayoung.inventorymanagementsystem.domain.redis;

import nayoung.inventorymanagementsystem.domain.item.ItemService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockItem {

    private RedissonClient redissonClient;
    private ItemService itemService;

    public RedissonLockItem(RedissonClient redissonClient, ItemService itemService) {
        this.redissonClient = redissonClient;
        this.itemService = itemService;
    }

    public void decreaseQuantity(Long itemId, Long quantity) {
        RLock lock = redissonClient.getLock(generateKey(itemId));
        try {
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);
            if(!available) {
                System.out.println("lock 획득 실패");
                return;
            }
            itemService.decreaseQuantity(itemId, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public String generateKey(Long key) {
        return key.toString();
    }
}
