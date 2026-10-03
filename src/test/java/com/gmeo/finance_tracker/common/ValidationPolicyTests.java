package com.gmeo.finance_tracker.common;
import com.gmeo.finance_tracker.common.validation.RequestValidation;
import com.gmeo.finance_tracker.auth.dto.RegisterRequest;
import com.gmeo.finance_tracker.transaction.dto.TransactionRequest;
import com.gmeo.finance_tracker.transaction.enums.TransactionType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
class ValidationPolicyTests {
    @Test void bcryptUsesUtf8Bytes() {
        var r = new RegisterRequest(); r.setEmail("a@example.com"); r.setFullName("A");
        for (String value : new String[]{"a".repeat(72), "\u0e01".repeat(24), "\ud83d\ude00".repeat(18)}) {
            r.setPassword(value); assertThatCode(() -> RequestValidation.validate(r)).doesNotThrowAnyException();
            r.setPassword(value + "a"); assertThatThrownBy(() -> RequestValidation.validate(r)).hasMessageContaining("72 UTF-8 bytes");
        }
    }
    @Test void moneyRejectsRoundingAndOverflow() {
        var r = new TransactionRequest(); r.setType(TransactionType.EXPENSE); r.setCategoryId(1L); r.setTransactionDate(LocalDate.now());
        for (String value : new String[]{"0.01", "99999999999999999.99"}) {
            r.setAmount(new BigDecimal(value)); assertThatCode(() -> RequestValidation.validate(r)).doesNotThrowAnyException();
        }
        for (String value : new String[]{"0", "-1", "1.001", "100000000000000000"}) {
            r.setAmount(new BigDecimal(value)); assertThatThrownBy(() -> RequestValidation.validate(r)).isInstanceOf(RuntimeException.class);
        }
    }
}
