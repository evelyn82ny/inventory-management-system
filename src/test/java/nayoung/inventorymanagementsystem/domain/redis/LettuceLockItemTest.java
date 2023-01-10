package nayoung.inventorymanagementsystem.domain.redis;

import nayoung.inventorymanagementsystem.domain.item.Item;
import nayoung.inventorymanagementsystem.domain.item.ItemRepository;
import nayoung.inventorymanagementsystem.domain.item.ItemService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LettuceLockItemTest {

    @Autowired
    private LettuceLockItem lettuceLockItem;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;

    private static final int threadCount = 30;

    @BeforeEach
    public void beforeEach() {
        itemService.create(30L); // quantity
    }

    @AfterEach
    public void afterEach() {
        itemRepository.deleteAll();
    }

    @Test
    public void decrease_quantity_test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    lettuceLockItem.decreaseQuantity(1L, 1L);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        Item item = itemRepository.findById(1L).orElseThrow();
        Assertions.assertEquals(0L, item.getQuantity());
    }
}