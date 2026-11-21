package com.loyalty.automation.service;

import com.loyalty.automation.action.ActionExecutor;
import com.loyalty.automation.domain.*;
import com.loyalty.automation.trigger.TriggerEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEngineService {

    private final AutomationRepository automationRepository;
    private final AutomationExecutionRepository executionRepository;
    private final AutomationActionRepository actionRepository;
    private final List<TriggerEvaluator> triggerEvaluators;
    private final List<ActionExecutor> actionExecutors;

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void runAutomations() {
        List<Automation> activeAutomations = automationRepository.findByEnabledTrue();
        for (Automation automation : activeAutomations) {
            evaluateAndExecute(automation);
        }
    }

    @Transactional
    public void evaluateAndExecute(Automation automation) {
        TriggerEvaluator evaluator = triggerEvaluators.stream()
                .filter(e -> e.supports(automation.getTriggerType()))
                .findFirst()
                .orElse(null);

        if (evaluator == null) {
            return;
        }

        List<UUID> matchingCustomers = evaluator.evaluate(automation);
        String today = LocalDate.now().toString();

        for (UUID customerId : matchingCustomers) {
            String idempotencyKey = automation.getId() + ":" + customerId + ":" + today;
            Optional<AutomationExecution> existing = executionRepository
                    .findByAutomationIdAndIdempotencyKey(automation.getId(), idempotencyKey);

            if (existing.isPresent()) {
                continue; // Prevent duplicate evaluation/spam
            }

            AutomationExecution execution = new AutomationExecution(
                    UUID.randomUUID(),
                    automation.getId(),
                    customerId,
                    null,
                    idempotencyKey
            );
            execution = executionRepository.save(execution);
            execution.markRunning();

            try {
                // Execute actions: Generate coupon & send email
                for (ActionType actionType : List.of(ActionType.GENERATE_COUPON, ActionType.SEND_EMAIL)) {
                    for (ActionExecutor executor : actionExecutors) {
                        if (executor.supports(actionType)) {
                            String result = executor.execute(automation, customerId);
                            AutomationAction action = new AutomationAction(
                                    UUID.randomUUID(),
                                    execution.getId(),
                                    actionType,
                                    "SUCCESS",
                                    result
                            );
                            actionRepository.save(action);
                        }
                    }
                }
                execution.markCompleted();
            } catch (Exception e) {
                log.error("Automation execution failed for automation {}", automation.getId(), e);
                execution.markFailed(e.getMessage());
            }
            executionRepository.save(execution);
        }
    }
}
