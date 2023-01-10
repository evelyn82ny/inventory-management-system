package nayoung.inventorymanagementsystem.domain.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    public final AccountRepository accountRepository;

    public void create() {
        Account account = new Account();
        accountRepository.save(account);
    }

    @Transactional
    public Long increaseBalance(Long accountId, Long amount) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.increaseBalance(amount);
        return account.getBalance();
    }
}
