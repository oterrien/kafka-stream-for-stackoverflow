package com.ote.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@EnableBinding(BindingProcessor.class)
@Slf4j
public class WithMongoDBConsumer {

    @Autowired
    private FileRepository fileRepository;

    @StreamListener(BindingProcessor.INPUT)
    public void onInputMessage(Message<String> commandMessage,
                               @Header(KafkaHeaders.ACKNOWLEDGMENT) Acknowledgment acknowledgment) throws Exception {

        Mono.just(commandMessage).
                map(msg -> msg.getPayload()).
                flatMap(fileId -> logReceiveCommand(fileId)).
                filterWhen(fileId -> fileRepository.existsByFileId(fileId)).
                flatMap(fileId -> fileRepository.findByFileId(fileId)).
                flatMap(fileDoc -> logProcessed(fileDoc)).
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

    private Mono<FileDocument> logProcessed(FileDocument fileDocument) {
        log.info("The file {} is processed", fileDocument.getFileId());
        return Mono.just(fileDocument);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Mono<Boolean> clean(FileDocument fileDocument) {
        if (fileDocument != null) {
            log.info("Remove file {} from database", fileDocument.getFileId());
            return fileRepository.deleteByFileId(fileDocument.getFileId());
        } else {
            log.info("File has already been removed from database");
            return Mono.just(false);
        }
    }
}
