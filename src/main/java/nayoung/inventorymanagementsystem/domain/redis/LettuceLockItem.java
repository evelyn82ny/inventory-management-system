package nayoung.inventorymanagementsystem.domain.redis;

import nayoung.inventorymanagementsystem.domain.item.ItemService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockItem {

    private RedisLockRepository redisLockRepository;
    private ItemService itemService;

    public LettuceLockItem(RedisLockRepository redisLockRepository, ItemService itemService) {
        this.redisLockRepository = redisLockRepository;
        this.itemService = itemService;
    }

    public void decreaseQuantity(Long itemId, Long quantity) throws InterruptedException {
        // busy-waiting
        while(!redisLockRepository.lock(itemId)) {
            Thread.sleep(100);
        }
        try {
            itemService.decreaseQuantity(itemId, quantity);
        } finally {
            redisLockRepository.unlock(itemId);
        }
    }
}
