package com.ote.test;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface BindingProcessor {

    String INPUT = "input";

    @Input(INPUT)
    SubscribableChannel inputFileWrite();

    String OUTPUT = "output";

    @Output(OUTPUT)
    MessageChannel outputFileWrite();
}
