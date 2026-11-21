package com.loyalty.automation.action;

import com.loyalty.automation.domain.ActionType;
import com.loyalty.automation.domain.Automation;

import java.util.UUID;

public interface ActionExecutor {
    boolean supports(ActionType actionType);
    String execute(Automation automation, UUID customerId);
}
