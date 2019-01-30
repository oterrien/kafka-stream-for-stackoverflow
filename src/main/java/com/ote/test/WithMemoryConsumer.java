package com.ote.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

@Service
@EnableBinding(BindingProcessor.class)
@Slf4j
public class WithMemoryConsumer {

    private final AtomicReference<String> repository = new AtomicReference<>("Test1");

    //    @StreamListener(BindingProcessor.INPUT)
    public void onInputMessage(Message<String> commandMessage,
                               @Header(KafkaHeaders.ACKNOWLEDGMENT) Acknowledgment acknowledgment) throws Exception {

        Mono.just(commandMessage).
                map(msg -> msg.getPayload()).
                flatMap(fileId -> logReceiveCommand(fileId)).
                filter(fileId -> repository.get().equalsIgnoreCase(fileId)).
                map(fileId -> repository.get()).
                flatMap(fileId -> logProcessed(fileId)).
                flatMap(fileDoc -> clean(fileDoc)).
                doOnTerminate(() -> acknowledge(acknowledgment)).
                block();
    }

    private void acknowledge(Acknowledgment acknowledgment) {
        acknowledgment.acknowledge();
    }

    private Mono<String> logReceiveCommand(String fileId) {
        log.info("Received command to process {}", fileId);
        return Mono.just(fileId);
    }

    private Mono<String> logProcessed(String fileId) {
        log.info("The file {} is processed", fileId);
        return Mono.just(fileId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Mono<Boolean> clean(String fileId) {
        if (fileId != null) {
            log.info("Removing file {} from database", fileId);
            repository.set("");
            return Mono.just(true);
        } else {
            log.info("File has already been removed from database");
            return Mono.just(false);
        }
    }
}
