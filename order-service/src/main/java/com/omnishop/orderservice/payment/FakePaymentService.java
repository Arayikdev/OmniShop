package com.omnishop.orderservice.payment;

import com.omnishop.orderservice.exception.InvalidCardFormatException;
import com.omnishop.orderservice.exception.InvalidCardNumberException;
import com.omnishop.orderservice.exception.PaymentDeclinedException;
import org.springframework.stereotype.Service;

@Service
public class FakePaymentService {

    public void processPayment(String cardNumber) {

        if (cardNumber == null || !cardNumber.matches("^[0-9]{16}$")) {
            throw new InvalidCardFormatException("Card must be 16 digits");
        }

        switch (cardNumber) {
            case "0000000000000000" -> throw new PaymentDeclinedException("Insufficient funds");
            case "1111111111111111" -> throw new PaymentDeclinedException("Card expired");
            case "2222222222222222" -> throw new PaymentDeclinedException("Card stolen or blocked");
        }

        if (!isValidLuhn(cardNumber)) {
            throw new InvalidCardNumberException("Invalid card number");
        }
    }

    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
