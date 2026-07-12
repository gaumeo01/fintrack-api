package com.gmeo.finance_tracker.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.account.dto.AccountRequest;
import com.gmeo.finance_tracker.account.dto.AccountResponse;
import com.gmeo.finance_tracker.account.enums.AccountType;
import com.gmeo.finance_tracker.common.exception.ResourceNotFoundException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.user.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AccountServiceTests {

    private AccountRepository accountRepository;
    private CurrentUserService currentUserService;
    private AccountService accountService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountRepository.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        accountService = new AccountService(accountRepository, currentUserService);

        currentUser = new User();
        currentUser.setId(7L);
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void createAccountAssignsCurrentUserAndDefaultsCurrentBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            account.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
            account.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
            return account;
        });

        AccountResponse response = accountService.createAccount(request("Cash", AccountType.CASH, "100.00"));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUser()).isEqualTo(currentUser);
        assertThat(accountCaptor.getValue().getCurrentBalance()).isEqualByComparingTo("100.00");
        assertThat(accountCaptor.getValue().isActive()).isTrue();
        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    void getAllAccountsListsOnlyCurrentUsersAccounts() {
        when(accountRepository.findAllByUserId(7L)).thenReturn(List.of(account(1L, "Cash")));

        List<AccountResponse> response = accountService.getAllAccounts();

        verify(accountRepository).findAllByUserId(7L);
        assertThat(response).extracting(AccountResponse::getName).containsExactly("Cash");
    }

    @Test
    void getAccountByIdCannotAccessAnotherUsersAccount() {
        when(accountRepository.findByIdAndUserId(9L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with id: 9");
    }

    @Test
    void updateAccountUpdatesOwnedAccount() {
        Account account = account(10L, "Cash");
        AccountRequest request = request("Main Bank", AccountType.BANK, "100.00");
        request.setCurrentBalance(new BigDecimal("125.50"));
        request.setActive(false);

        when(accountRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountResponse response = accountService.updateAccount(10L, request);

        assertThat(account.getName()).isEqualTo("Main Bank");
        assertThat(account.getType()).isEqualTo(AccountType.BANK);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("125.50");
        assertThat(account.isActive()).isFalse();
        assertThat(response.getName()).isEqualTo("Main Bank");
    }

    @Test
    void deleteAccountCannotAccessAnotherUsersAccount() {
        when(accountRepository.findByIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deleteAccount(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found with id: 10");

        verify(accountRepository, never()).delete(any(Account.class));
    }

    private AccountRequest request(String name, AccountType type, String initialBalance) {
        AccountRequest request = new AccountRequest();
        request.setName(name);
        request.setType(type);
        request.setInitialBalance(new BigDecimal(initialBalance));
        return request;
    }

    private Account account(Long id, String name) {
        Account account = new Account();
        account.setId(id);
        account.setName(name);
        account.setType(AccountType.CASH);
        account.setInitialBalance(new BigDecimal("100.00"));
        account.setCurrentBalance(new BigDecimal("100.00"));
        account.setActive(true);
        account.setUser(currentUser);
        account.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        account.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        return account;
    }
}
