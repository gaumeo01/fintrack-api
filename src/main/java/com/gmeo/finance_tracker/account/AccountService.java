package com.gmeo.finance_tracker.account;

import com.gmeo.finance_tracker.account.dto.AccountRequest;
import com.gmeo.finance_tracker.account.dto.AccountResponse;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    public AccountService(AccountRepository accountRepository, CurrentUserService currentUserService) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
    }

    public AccountResponse createAccount(AccountRequest request) {
        Account account = new Account();
        account.setUser(currentUserService.getCurrentUser());
        applyRequest(account, request);

        return mapToResponse(accountRepository.save(account));
    }

    public List<AccountResponse> getAllAccounts() {
        User currentUser = currentUserService.getCurrentUser();
        return accountRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long id) {
        return mapToResponse(findOwnedAccount(id));
    }

    public AccountResponse updateAccount(Long id, AccountRequest request) {
        Account account = findOwnedAccount(id);
        applyRequest(account, request);

        return mapToResponse(accountRepository.save(account));
    }

    public void deleteAccount(Long id) {
        accountRepository.delete(findOwnedAccount(id));
    }

    private void applyRequest(Account account, AccountRequest request) {
        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setCurrentBalance(resolveCurrentBalance(request));
        if (request.getActive() != null) {
            account.setActive(request.getActive());
        }
    }

    private BigDecimal resolveCurrentBalance(AccountRequest request) {
        if (request.getCurrentBalance() != null) {
            return request.getCurrentBalance();
        }
        return request.getInitialBalance();
    }

    private Account findOwnedAccount(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        return accountRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setInitialBalance(account.getInitialBalance());
        response.setCurrentBalance(account.getCurrentBalance());
        response.setActive(account.isActive());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }
}
