package ru.mirea.officebooking.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonInputConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonInput() {
        return builder -> builder
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                        JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .postConfigurer(mapper -> {
                    for (var type : new LogicalType[]{LogicalType.Integer, LogicalType.Boolean}) {
                        mapper.coercionConfigFor(type).setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail);
                    }
                    mapper.coercionConfigFor(LogicalType.Boolean)
                            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                    var text = mapper.coercionConfigFor(LogicalType.Textual);
                    text.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
                    text.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
                    text.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                });
    }
}
