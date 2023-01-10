package nayoung.inventorymanagementsystem.domain.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public void create(Long quantity) {
        Item item = new Item(quantity);
        itemRepository.save(item);
    }

    @Transactional
    public void decreaseQuantity(Long id, Long quantity) {
        Item item = itemRepository.findByIdWithPessimisticLock(id);
        if(!item.decreaseQuantity(quantity))
            throw new IllegalArgumentException();
    }
}
