package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.deserializer;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class PowerDeserializer extends JsonDeserializer<Power> {

    @Override
    public Power deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        try {
            return Power.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IOException("Valor inválido para Power enum: " + value);
        }
    }
}
