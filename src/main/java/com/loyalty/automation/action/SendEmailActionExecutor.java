package com.loyalty.automation.action;

import com.loyalty.automation.domain.ActionType;
import com.loyalty.automation.domain.Automation;
import com.loyalty.customer.domain.Customer;
import com.loyalty.customer.domain.CustomerRepository;
import com.loyalty.notification.email.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SendEmailActionExecutor implements ActionExecutor {

    private final EmailNotificationService emailService;
    private final CustomerRepository customerRepository;

    @Override
    public boolean supports(ActionType actionType) {
        return actionType == ActionType.SEND_EMAIL;
    }

    @Override
    public String execute(Automation automation, UUID customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer != null && customer.getEmail() != null) {
            String subject = "Te extrañamos en nuestro local!";
            String body = "Hola " + customer.getName() + ", tenemos un beneficio especial para tu próxima visita.";
            emailService.sendEmail(customer.getEmail(), subject, body);
            return "{\"emailSentTo\":\"" + customer.getEmail() + "\"}";
        }
        return "{\"emailSkipped\":\"No valid email profile\"}";
    }
}
