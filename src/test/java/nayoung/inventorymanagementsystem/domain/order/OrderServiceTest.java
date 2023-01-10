package nayoung.inventorymanagementsystem.domain.order;

import nayoung.inventorymanagementsystem.domain.account.AccountService;
import nayoung.inventorymanagementsystem.domain.item.Item;
import nayoung.inventorymanagementsystem.domain.item.ItemRepository;
import nayoung.inventorymanagementsystem.domain.item.ItemService;
import nayoung.inventorymanagementsystem.web.order.model.RequestOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private AccountService accountService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private OrderService orderService;

    private static final int threadCount = 30;

    @BeforeEach
    public void beforeEach() {
        for (int i = 0; i < threadCount; i++)
            accountService.create();

        itemService.create(6L);
    }

    @Test
    @DisplayName("Deadlock 발생")
    public void order() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long accountId = i;
            executorService.submit(() -> {
                try {
                    orderService.create(accountId, new RequestOrder(1L, 1L));
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