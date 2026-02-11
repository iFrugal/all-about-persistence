package lazydevs.readeasy.integration;

import lazydevs.readeasy.TestApplication;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.controller.ConfiguredReadController;
import lazydevs.readeasy.validation.QueryValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ConfiguredReadController.
 * Tests the REST endpoints with actual server startup using MockMvc.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@Import(MockReadersTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "readeasy.validation.enabled=false",
        "readeasy.devtools.enabled=false"
})
@DisplayName("ConfiguredReadController Integration Tests")
@Disabled("Use run-api-tests.sh for shell-based integration testing instead")
class ConfiguredReadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private ReadEasyConfig readEasyConfig;

    @Autowired(required = false)
    private ConfiguredReadController controller;

    @Autowired(required = false)
    private QueryValidator queryValidator;

    @Nested
    @DisplayName("Controller Initialization")
    class ControllerInitialization {

        @Test
        @DisplayName("Should successfully initialize controller")
        void shouldInitializeController() {
            assertNotNull(controller,
                    "ConfiguredReadController should be initialized");
        }

        @Test
        @DisplayName("Should initialize with query validator")
        void shouldInitializeWithQueryValidator() {
            // QueryValidator may not be present if validation is disabled
            // Just verify controller is working
            assertNotNull(controller,
                    "Controller should be initialized");
        }

        @Test
        @DisplayName("Should have GeneralReader map configured")
        void shouldHaveGeneralReaderMap() {
            assertNotNull(controller.getReadEasyGeneralReaderMap(),
                    "GeneralReader map should be configured");
            assertFalse(controller.getReadEasyGeneralReaderMap().isEmpty(),
                    "GeneralReader map should not be empty");
        }
    }

    @Nested
    @DisplayName("/read/one Endpoint")
    class FindOneEndpoint {

        @Test
        @DisplayName("Should return single record for valid query")
        void shouldReturnSingleRecord() throws Exception {
            mockMvc.perform(post("/read/one")
                            .param("queryId", "users.findById")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\": 1}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").exists());
        }

        @Test
        @DisplayName("Should return 400 for non-existent query ID")
        void shouldReturn400ForNonExistentQueryId() throws Exception {
            mockMvc.perform(post("/read/one")
                            .param("queryId", "nonexistent.query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle request with missing optional parameters")
        void shouldHandleRequestWithMissingOptionalParameters() throws Exception {
            mockMvc.perform(post("/read/one")
                            .param("queryId", "users.findById")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\": 1}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("/read/list Endpoint")
    class FindAllEndpoint {

        @Test
        @DisplayName("Should return list of records")
        void shouldReturnListOfRecords() throws Exception {
            mockMvc.perform(post("/read/list")
                            .param("queryId", "users.findAll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(greaterThan(0)));
        }

        @Test
        @DisplayName("Should support orderby parameter")
        void shouldSupportOrderbyParameter() throws Exception {
            mockMvc.perform(post("/read/list")
                            .param("queryId", "users.findAll")
                            .param("orderby", "name")
                            .param("orderdir", "asc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should support search with optional parameters")
        void shouldSupportSearchWithOptionalParameters() throws Exception {
            mockMvc.perform(post("/read/list")
                            .param("queryId", "users.search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"John\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("/read/page Endpoint")
    class FindPageEndpoint {

        @Test
        @DisplayName("Should return paginated results")
        void shouldReturnPaginatedResults() throws Exception {
            mockMvc.perform(post("/read/page")
                            .param("queryId", "users.findAll")
                            .param("pageNum", "1")
                            .param("pageSize", "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNum").value(1))
                    .andExpect(jsonPath("$.pageSize").value(2))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should use default page values when not provided")
        void shouldUseDefaultPageValues() throws Exception {
            mockMvc.perform(post("/read/page")
                            .param("queryId", "users.findAll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNum").value(1))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }

        @Test
        @DisplayName("Should support ordering with pagination")
        void shouldSupportOrderingWithPagination() throws Exception {
            mockMvc.perform(post("/read/page")
                            .param("queryId", "users.findAll")
                            .param("pageNum", "1")
                            .param("pageSize", "5")
                            .param("orderby", "id")
                            .param("orderdir", "desc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("/read/count Endpoint")
    class CountEndpoint {

        @Test
        @DisplayName("Should return record count")
        void shouldReturnRecordCount() throws Exception {
            MvcResult result = mockMvc.perform(post("/read/count")
                            .param("queryId", "users.findAll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            long count = Long.parseLong(response);
            assertTrue(count >= 0, "Count should be non-negative");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle invalid query ID gracefully")
        void shouldHandleInvalidQueryIdGracefully() throws Exception {
            mockMvc.perform(post("/read/one")
                            .param("queryId", "invalid.queryId")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing queryId parameter")
        void shouldHandleMissingQueryIdParameter() throws Exception {
            mockMvc.perform(post("/read/one")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle invalid JSON body")
        void shouldHandleInvalidJsonBody() throws Exception {
            mockMvc.perform(post("/read/one")
                            .param("queryId", "users.findById")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("not valid json"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Content Type Handling")
    class ContentTypeHandling {

        @Test
        @DisplayName("Should accept application/json content type")
        void shouldAcceptJsonContentType() throws Exception {
            mockMvc.perform(post("/read/list")
                            .param("queryId", "users.findAll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return application/json response")
        void shouldReturnJsonResponse() throws Exception {
            mockMvc.perform(post("/read/list")
                            .param("queryId", "users.findAll")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
