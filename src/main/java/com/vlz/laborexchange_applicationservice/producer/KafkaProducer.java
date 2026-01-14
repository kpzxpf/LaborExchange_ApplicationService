package com.vlz.laborexchange_applicationservice.producer;

public interface KafkaProducer<T> {
    void send(T event);
}