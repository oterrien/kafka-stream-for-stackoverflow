package com.ote.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.stream.IntStream;

@SpringBootApplication
@EnableBinding(BindingProcessor.class)
@EnableTransactionManagement
@EnableWebFlux
@Slf4j
public class ConsumerApplicationRunner {

    public static void main(String... args) {
        SpringApplication.run(ConsumerApplicationRunner.class, args);
    }

    @Autowired
    private BindingProcessor bindingProcessor;

    @Autowired
    private FileRepository fileRepository;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {

            cleanAll().
                    then(Mono.just(new FileDocument())).
                    map(fileDocument -> {
                        fileDocument.setFileId("Test1");
                        return fileDocument;
                    }).
                    flatMap(fileDocument -> create(fileDocument)).
                    flatMap(fileDocument -> publishCommand(fileDocument, 10)).
                    subscribeOn(Schedulers.immediate()).
                    subscribe();
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Mono<Void> cleanAll() {
        log.info("Cleaning all files");
        return fileRepository.deleteAll();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Mono<FileDocument> create(FileDocument fileDocument) {
        log.info("Creating file {}", fileDocument.getFileId());
        return fileRepository.save(fileDocument);
    }

    private Mono<Boolean> publishCommand(FileDocument fileDocument, int numMessage) {

        //return Flux.fromIterable(IntStream.range(0, numMessage).mapToObj(i -> fileDocument.getFileId()).collect(Collectors.toList())).
        return Flux.fromStream(IntStream.range(0, numMessage).mapToObj(i -> fileDocument.getFileId())).
                map(cmd -> {
                    log.info("Send command for {}", cmd);
                    return MessageBuilder.withPayload(cmd).build();
                }).
                map(msg -> bindingProcessor.outputFileWrite().send(msg)).
                collectList().
                map(list -> list.stream().allMatch(p -> p));
    }
}