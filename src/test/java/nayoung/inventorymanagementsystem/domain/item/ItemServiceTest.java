package nayoung.inventorymanagementsystem.domain.item;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemServiceTest {

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
    @DisplayName("synchronized 사용해 Lost update 해결")
    public void decrease_quantity_test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    itemService.decreaseQuantity(1L, 1L);
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