package io.github.dmitriyiliyov.springoutbox.publisher.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Objects;

@Aspect
public class OutboxPublishAspect {

    private final ExpressionParser expressionParser;
    private final ApplicationEventPublisher eventPublisher;

    public OutboxPublishAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.expressionParser = new SpelExpressionParser();
    }

    @Pointcut("@annotation(outboxPublish) && execution(public * *(..))")
    public void pointcut(OutboxPublish outboxPublish) {}

    @AfterReturning(
            pointcut = "pointcut(outboxPublish)",
            returning = "result",
            argNames = "joinPoint,outboxPublish,result"
    )
    public void advice(JoinPoint joinPoint, OutboxPublish outboxPublish, Object result) {
        Object payload = result;
        String spelPayload = outboxPublish.payload();
        if (spelPayload != null && !spelPayload.isBlank() && !spelPayload.equals("#result")) {
            StandardEvaluationContext context = getContext(joinPoint);
            context.setVariable("result", result);
            payload = expressionParser.parseExpression(spelPayload).getValue(context);
        }
        Objects.requireNonNull(payload, "payload cannot be null");
        if (payload instanceof List<?>) {
            eventPublisher.publishEvent(new RowOutboxEvents(outboxPublish.eventType(), (List<?>) payload));
            return;
        }
        eventPublisher.publishEvent(new RowOutboxEvent(outboxPublish.eventType(), payload));
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

