package nayoung.inventorymanagementsystem.domain.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public void create(Long quantity) {
        Item item = new Item(quantity);
        itemRepository.save(item);
    }

    public synchronized void decreaseQuantity(Long id, Long quantity) {
        Item item = itemRepository.findById(id).orElseThrow();
        item.decreaseQuantity(quantity);
        itemRepository.saveAndFlush(item);
    }
}
