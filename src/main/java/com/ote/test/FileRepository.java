package com.ote.test;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface FileRepository extends ReactiveMongoRepository<FileDocument, String> {

    @Query(value = "{ file_id: ?0 }")
    Mono<FileDocument> findByFileId(String fileId);

    @Query(value = "{ file_id: ?0 }", delete = true)
    Mono<Boolean> deleteByFileId(String fileId);

    @Query(value = "{ file_id: ?0 }", exists = true)
    Mono<Boolean> existsByFileId(String fileId);
}
