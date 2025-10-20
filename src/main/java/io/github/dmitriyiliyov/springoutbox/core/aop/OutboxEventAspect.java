package io.github.dmitriyiliyov.springoutbox.core.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Aspect
@Component
public class OutboxEventAspect {

    private final ExpressionParser expressionParser;
    private final ApplicationEventPublisher eventPublisher;

    public OutboxEventAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.expressionParser = new SpelExpressionParser();
    }

    @Pointcut("@annotation(outboxEvent) && execution(public * *(..))")
    public void publishEventPointcut(OutboxEvent outboxEvent) {}

    @AfterReturning(
            pointcut = "publishEventPointcut(outboxEvent)",
            returning = "result",
            argNames = "joinPoint,outboxEvent,result"
    )
    public void publishEvent(JoinPoint joinPoint, OutboxEvent outboxEvent, Object result) {
        StandardEvaluationContext context = getContext(joinPoint);
        context.setVariable("result", result);
        Object payload = result;
        String spelPayload = outboxEvent.payload();
        if (spelPayload != null && !spelPayload.isBlank()) {
            payload = expressionParser.parseExpression(spelPayload).getValue(context);
        }
        Objects.requireNonNull(payload, "payload cannot be null");
        if (payload instanceof List<?>) {
            eventPublisher.publishEvent(new RowOutboxEvents(outboxEvent.eventType(), (List<?>) payload));
            return;
        }
        eventPublisher.publishEvent(new RowOutboxEvent(outboxEvent.eventType(), payload));
    }

    private StandardEvaluationContext getContext(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            if (paramNames != null && i < paramNames.length) {
                context.setVariable(paramNames[i], args[i]);
            } else {
                context.setVariable("args[" + i + "]", args[i]);
            }
        }
        return context;
    }
}
