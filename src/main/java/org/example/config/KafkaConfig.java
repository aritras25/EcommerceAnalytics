package org.example.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.nio.charset.StandardCharsets;

/**
 * KafkaConfig with DeadLetterPublishingRecoverer using KafkaOperations<Object,Object>
 * and DefaultErrorHandler with exponential backoff and DLQ publishing.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:liftlab-analytics}")
    private String consumerGroupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String consumerAutoOffset;

    @Value("${spring.kafka.consumer.concurrency:3}")
    private int consumerConcurrency;

    // retry/backoff props
    @Value("${kafka.consumer.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${kafka.consumer.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${kafka.consumer.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${kafka.consumer.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    @Value("${kafka.dlq.topic:events.DLT}")
    private String dltTopic;

    // producer tuning for dead-letter publishing
    @Value("${spring.kafka.producer.acks:1}")
    private String producerAcks;

    @Value("${spring.kafka.producer.linger-ms:5}")
    private int producerLingerMs;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private int producerBatchSize;

    // --- Consumer factory ---
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String,Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // add other tuning if needed
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // --- Producer factory & template ---
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String,Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, producerAcks);
        props.put(ProducerConfig.LINGER_MS_CONFIG, producerLingerMs);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }

    /**
     * DeadLetterPublishingRecoverer using KafkaOperations<Object,Object>.
     * We cast the typed KafkaTemplate<String,String> to KafkaOperations<Object,Object>
     * when creating this bean to satisfy the constructor signature.
     */
//    @Bean
//    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaOperations<Object, Object> kafkaOps) {
//        // Destination resolver: send to configured DLT topic, preserve partition if possible
//        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver = (record, ex) -> {
//            int partition = record.partition();
//            // if partition < 0, return with -1 -> DLPR will set null partition
//            return new TopicPartition(dltTopic, partition < 0 ? -1 : partition);
//        };
//
//        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(kafkaOps, destinationResolver);
//
//        // Add headersFunction to include original topic/partition/offset and exception message
//        dlt.addHeadersFunction((record, ex) -> {
//            RecordHeaders headers = new RecordHeaders();
//            try {
//                headers.add(new RecordHeader("x-original-topic", record.topic().getBytes(StandardCharsets.UTF_8)));
//                headers.add(new RecordHeader("x-original-partition", Integer.toString(record.partition()).getBytes(StandardCharsets.UTF_8)));
//                headers.add(new RecordHeader("x-original-offset", Long.toString(record.offset()).getBytes(StandardCharsets.UTF_8)));
//                String exMsg = ex == null ? "null" : ex.getClass().getName() + ": " + ex.getMessage();
//                headers.add(new RecordHeader("x-exception-message", exMsg.getBytes(StandardCharsets.UTF_8)));
//            } catch (Exception ignore) {
//                // best-effort header creation
//            }
//            return headers;
//        });
//
//        // Other optional configurations (example: don't wait forever)
//        dlt.setFailIfSendResultIsError(true);
//        dlt.setWaitForSendResultTimeout(Duration.ofSeconds(30));
//        return dlt;
//    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaOperations<String, String> kafkaTemplate) {
        // Example: publish to topic: original-topic.DLT
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    }

    /**
     * DefaultErrorHandler with ExponentialBackOffWithMaxRetries.
     * Retries = maxAttempts - 1 (first attempt + retries = attempts).
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer dltRecoverer) {
        // Exponential backoff with max retries
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxIntervalMs);

        DefaultErrorHandler handler = new DefaultErrorHandler(dltRecoverer, backOff);

        // Example: treat IllegalArgumentException as non-retryable (send straight to DLQ)
        handler.addNotRetryableExceptions(IllegalArgumentException.class);

        // Optional retry listener for visibility
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.out.println("Retry attempt=" + deliveryAttempt +
                    " topic=" + record.topic() +
                    " partition=" + record.partition() +
                    " offset=" + record.offset() +
                    " exception=" + ex.getMessage());
        });

        return handler;
    }

    // --- Container factory with manual immediate ack and error handler wired ---
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(consumerConcurrency);

        // Manual immediate ack - call ack.acknowledge() in consumer after success
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);

        // Wire the DefaultErrorHandler (retries + DLQ)
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}



