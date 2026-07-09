package com.gmeo.finance_tracker.account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gmeo.finance_tracker.account.dto.AccountRequest;
import com.gmeo.finance_tracker.account.dto.AccountResponse;
import com.gmeo.finance_tracker.account.enums.AccountType;
import com.gmeo.finance_tracker.auth.JwtService;
import com.gmeo.finance_tracker.common.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class AccountControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void createAccountReturnsCreatedForValidRequest() throws Exception {
        AccountResponse response = new AccountResponse();
        response.setId(1L);
        response.setName("Cash");
        response.setType(AccountType.CASH);
        response.setInitialBalance(new BigDecimal("100.00"));
        response.setCurrentBalance(new BigDecimal("100.00"));
        response.setActive(true);
        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cash",
                                  "type": "CASH",
                                  "initialBalance": 100.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Cash"))
                .andExpect(jsonPath("$.type").value("CASH"))
                .andExpect(jsonPath("$.initialBalance").value(100.00))
                .andExpect(jsonPath("$.currentBalance").value(100.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createAccountReturnsBadRequestForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentBalance": 50.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.type").exists())
                .andExpect(jsonPath("$.validationErrors.initialBalance").exists());

        verifyNoInteractions(accountService);
    }

    @Test
    void createAccountReturnsBadRequestForNameOverMaxLength() throws Exception {
        String name = "a".repeat(101);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "type": "CASH",
                                  "initialBalance": 100.00
                                }
                                """.formatted(name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());

        verifyNoInteractions(accountService);
    }
}
