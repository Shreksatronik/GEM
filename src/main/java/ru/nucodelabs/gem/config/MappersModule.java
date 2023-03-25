package ru.nucodelabs.gem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.mapstruct.factory.Mappers;
import ru.nucodelabs.gem.file.dto.mapper.DtoMapper;
import ru.nucodelabs.gem.fxmodel.anisotropy.mapper.AnisotropyFxModelMapper;
import ru.nucodelabs.gem.fxmodel.ves.mapper.VesFxModelMapper;

import static com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper;

public class MappersModule extends AbstractModule {
    @Provides
    @Singleton
    VesFxModelMapper fxModelMapper() {
        return Mappers.getMapper(VesFxModelMapper.class);
    }

    @Provides
    @Singleton
    AnisotropyFxModelMapper anisotropyFxModelMapper() {
        return Mappers.getMapper(AnisotropyFxModelMapper.class);
    }

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Provides
    @Singleton
    DtoMapper dtoMapper(Injector injector) {
        return injector.getInstance(Mappers.getMapperClass(DtoMapper.class));
    }
}