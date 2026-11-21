package com.loyalty.automation.trigger;

import com.loyalty.automation.domain.Automation;
import com.loyalty.automation.domain.TriggerType;

import java.util.List;
import java.util.UUID;

public interface TriggerEvaluator {
    boolean supports(TriggerType triggerType);
    List<UUID> evaluate(Automation automation);
}
