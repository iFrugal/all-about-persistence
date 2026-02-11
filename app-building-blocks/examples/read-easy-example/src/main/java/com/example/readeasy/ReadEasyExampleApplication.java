package com.example.readeasy;

import lazydevs.readeasy.controller.ConfiguredReadController;
import lazydevs.services.basic.handler.RESTExceptionHandler;
import lazydevs.services.basic.validation.ParamValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Example Spring Boot application demonstrating Read-Easy framework usage.
 *
 * <p>This application shows how to:</p>
 * <ul>
 *   <li>Configure Read-Easy with mock readers (see {@link com.example.readeasy.config.MockReadersConfig})</li>
 *   <li>Define query files in YAML format (see resources/queries/)</li>
 *   <li>Expose database queries as REST endpoints</li>
 * </ul>
 *
 * <h2>Key Imports:</h2>
 * <ul>
 *   <li>{@link ConfiguredReadController} - REST controller providing /read/* endpoints</li>
 *   <li>{@link DynaBeansAutoConfiguration} - Dynamic bean creation support</li>
 *   <li>{@link RESTExceptionHandler} - Maps ValidationException to HTTP 400</li>
 * </ul>
 *
 * <h2>Running the Application:</h2>
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <h2>Testing the APIs:</h2>
 * <pre>
 * curl -X POST http://localhost:8080/read/list?queryId=users.findAll \
 *   -H "Content-Type: application/json" -d '{}'
 * </pre>
 */
@SpringBootApplication
@Import({
    ConfiguredReadController.class,      // REST endpoints for query execution
    DynaBeansAutoConfiguration.class,    // Dynamic beans support
    RESTExceptionHandler.class,          // Proper HTTP error codes (400 for validation errors)
    ParamValidator.class                 // Parameter validation
})
public class ReadEasyExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadEasyExampleApplication.class, args);
    }
}
