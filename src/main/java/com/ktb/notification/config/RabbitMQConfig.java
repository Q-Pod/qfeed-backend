package com.ktb.notification.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@EnableConfigurationProperties({RabbitMQProperties.class, OutboxRelayProperties.class})
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    public RabbitMQConfig(RabbitMQProperties props) {
        this.props = props;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public DirectExchange notificationDirectExchange() {
        return new DirectExchange(props.getExchanges().getDirect(), true, false);
    }

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(props.getExchanges().getDlx(), true, false);
    }

    @Bean
    public Queue campaignExecutionQueue() {
        return QueueBuilder.durable(props.getQueues().getCampaignExecution())
            .withArgument("x-dead-letter-exchange", props.getExchanges().getDlx())
            .withArgument("x-dead-letter-routing-key",
                props.getRoutingKeys().getDlqCampaignExecution())
            .withArgument("x-message-ttl", 86_400_000L)
            .build();
    }

    @Bean
    public Queue notificationRequestedQueue() {
        return QueueBuilder.durable(props.getQueues().getNotificationRequested())
            .withArgument("x-dead-letter-exchange", props.getExchanges().getDlx())
            .withArgument("x-dead-letter-routing-key",
                props.getRoutingKeys().getDlqNotificationRequested())
            .withArgument("x-message-ttl", 86_400_000L)
            .build();
    }

    @Bean
    public Queue noticePublishedQueue() {
        return QueueBuilder.durable(props.getQueues().getNoticePublished())
            .withArgument("x-dead-letter-exchange", props.getExchanges().getDlx())
            .withArgument("x-dead-letter-routing-key",
                props.getRoutingKeys().getDlqNoticePublished())
            .withArgument("x-message-ttl", 86_400_000L)
            .build();
    }

    @Bean
    public Queue dlqCampaignExecutionQueue() {
        return QueueBuilder.durable(props.getQueues().getDlqCampaignExecution()).build();
    }

    @Bean
    public Queue dlqNotificationRequestedQueue() {
        return QueueBuilder.durable(props.getQueues().getDlqNotificationRequested()).build();
    }

    @Bean
    public Queue dlqNoticePublishedQueue() {
        return QueueBuilder.durable(props.getQueues().getDlqNoticePublished()).build();
    }

    @Bean
    public Binding campaignExecutionBinding() {
        return BindingBuilder
            .bind(campaignExecutionQueue())
            .to(notificationDirectExchange())
            .with(props.getRoutingKeys().getCampaignExecution());
    }

    @Bean
    public Binding notificationRequestedBinding() {
        return BindingBuilder
            .bind(notificationRequestedQueue())
            .to(notificationDirectExchange())
            .with(props.getRoutingKeys().getNotificationRequested());
    }

    @Bean
    public Binding noticePublishedBinding() {
        return BindingBuilder
            .bind(noticePublishedQueue())
            .to(notificationDirectExchange())
            .with(props.getRoutingKeys().getNoticePublished());
    }

    @Bean
    public Binding dlqCampaignExecutionBinding() {
        return BindingBuilder
            .bind(dlqCampaignExecutionQueue())
            .to(notificationDlx())
            .with(props.getRoutingKeys().getDlqCampaignExecution());
    }

    @Bean
    public Binding dlqNotificationRequestedBinding() {
        return BindingBuilder
            .bind(dlqNotificationRequestedQueue())
            .to(notificationDlx())
            .with(props.getRoutingKeys().getDlqNotificationRequested());
    }

    @Bean
    public Binding dlqNoticePublishedBinding() {
        return BindingBuilder
            .bind(dlqNoticePublishedQueue())
            .to(notificationDlx())
            .with(props.getRoutingKeys().getDlqNoticePublished());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory manualAckListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }
}
