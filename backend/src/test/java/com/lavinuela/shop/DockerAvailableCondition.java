package com.lavinuela.shop;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Condición de {@link RequiresDocker}: activa las pruebas solo si Docker está disponible. */
class DockerAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return DockerSupport.isDockerAvailable()
                ? ConditionEvaluationResult.enabled("Docker disponible")
                : ConditionEvaluationResult.disabled("Docker no está disponible: se salta la prueba");
    }
}
