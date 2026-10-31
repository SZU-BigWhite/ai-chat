package com.aichat;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.web.bind.annotation.RestController;

/** 防止后续改动重新引入控制器访问数据库或实体公开字段。 */
class BackendArchitectureTest {
    private List<Class<?>> applicationClasses() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        var readerFactory = new CachingMetadataReaderFactory(resolver);
        List<Class<?>> classes = new ArrayList<>();
        for (var resource : resolver.getResources("classpath*:com/aichat/**/*.class")) {
            String name = readerFactory.getMetadataReader(resource).getClassMetadata().getClassName();
            classes.add(Class.forName(name, false, getClass().getClassLoader()));
        }
        return classes;
    }

    @Test
    void controllersDoNotDependOnRepositoriesOrEntities() throws Exception {
        var controllers =
            applicationClasses().stream().filter(type -> type.isAnnotationPresent(RestController.class)).toList();
        assertThat(controllers).hasSize(5);
        for (Class<?> controller : controllers) {
            for (var field : controller.getDeclaredFields()) {
                assertThat(field.getType().getPackageName()).doesNotContain("repository", "model.entity");
            }
            for (var method : controller.getDeclaredMethods()) {
                assertThat(method.getGenericReturnType().getTypeName()).doesNotContain("model.entity", "java.util.Map");
                for (Class<?> parameter : method.getParameterTypes()) {
                    assertThat(parameter.getPackageName()).doesNotContain("model.entity");
                }
            }
        }
    }

    @Test
    void entityFieldsArePrivateAndTablesRemainExplicit() throws Exception {
        var entities = applicationClasses().stream().filter(type -> type.isAnnotationPresent(Entity.class)).toList();
        assertThat(entities).hasSize(7);
        for (Class<?> entity : entities) {
            assertThat(entity.getSimpleName()).endsWith("DO");
            assertThat(entity.getAnnotation(Table.class)).isNotNull();
            assertThat(entity.getAnnotation(Table.class).name()).isNotBlank();
            for (var field : entity.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                assertThat(Modifier.isPrivate(field.getModifiers())).as(entity.getName() + "." + field.getName())
                    .isTrue();
                assertThat(field.getType().isPrimitive()).isFalse();
            }
        }
    }

    @Test
    void serviceContractsHaveSeparateImplementations() throws Exception {
        var contracts =
            applicationClasses().stream().filter(type -> type.getPackageName().equals("com.aichat.service")).toList();
        assertThat(contracts).hasSize(5);
        for (Class<?> contract : contracts) {
            assertThat(contract.isInterface()).isTrue();
            Class<?> implementation = Class.forName("com.aichat.service.impl." + contract.getSimpleName() + "Impl");
            assertThat(contract.isAssignableFrom(implementation)).isTrue();
        }
    }
}
