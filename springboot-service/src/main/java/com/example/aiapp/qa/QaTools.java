package com.example.aiapp.qa;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Tool surface for the Tool route - arithmetic + current date/time, matching
 * fastapi-service/qa_graph/tools.py's TOOLS.
 */
@Component
public class QaTools {

    public static final String CALCULATOR = "calculator";
    public static final String CURRENT_DATETIME = "current_datetime";

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public static List<ChatCompletionTool> definitions() {
        FunctionParameters calculatorParams = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "expression", Map.of(
                                "type", "string",
                                "description", "The arithmetic expression to evaluate, e.g. '12 * (4 + 3)'"))))
                .putAdditionalProperty("required", JsonValue.from(List.of("expression")))
                .build();

        FunctionDefinition calculatorDefinition = FunctionDefinition.builder()
                .name(CALCULATOR)
                .description("Evaluates a basic arithmetic expression. Only numbers and the "
                        + "+ - * / ** % operators are supported.")
                .parameters(calculatorParams)
                .build();

        FunctionDefinition datetimeDefinition = FunctionDefinition.builder()
                .name(CURRENT_DATETIME)
                .description("Returns the current UTC date and time.")
                .parameters(FunctionParameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty("properties", JsonValue.from(Map.of()))
                        .build())
                .build();

        return List.of(
                ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder().function(calculatorDefinition).build()),
                ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder().function(datetimeDefinition).build()));
    }

    /** Evaluates a basic arithmetic expression, e.g. "12 * (4 + 3)". */
    public String calculate(String expression) {
        try {
            double result = new SafeArithmeticEvaluator(expression).evaluate();
            return (result == Math.floor(result) && !Double.isInfinite(result))
                    ? String.valueOf((long) result)
                    : String.valueOf(result);
        } catch (RuntimeException e) {
            return "Could not evaluate '" + expression + "': " + e.getMessage();
        }
    }

    /** Returns the current UTC date and time. */
    public String currentDateTime() {
        return DATETIME_FORMAT.format(Instant.now()) + " UTC";
    }
}
