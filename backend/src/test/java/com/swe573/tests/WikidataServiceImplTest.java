package com.swe573.tests;

import com.swe573.dto.WikidataEntityDTO;
import com.swe573.services.WikidataService;
import com.swe573.services.impl.WikidataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikidataServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WikidataServiceImpl wikidataService;

    @BeforeEach
    void setUp() {
        // No need to manually set RestTemplate as it's injected by @InjectMocks
    }

    @Test
    void getEntityById_WithValidId_ReturnsEntityInfo() {
        Map<String, Object> mockResponse = Map.of(
            "entities", Map.of(
                "Q42", Map.of(
                    "id", "Q42",
                    "labels", Map.of("en", Map.of("value", "Douglas Adams")),
                    "descriptions", Map.of("en", Map.of("value", "English writer and humorist")),
                    "claims", Map.of(
                        "P31", List.of(Map.of(
                            "mainsnak", Map.of(
                                "datavalue", Map.of(
                                    "value", Map.of(
                                        "entity-type", "item",
                                        "numeric-id", 5
                                    )
                                )
                            ),
                            "type", "statement"
                        ))
                    )
                )
            )
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        WikidataEntityDTO result = wikidataService.getEntityById("Q42");

        assertNotNull(result);
        assertEquals("Q42", result.getId());
        assertEquals("Douglas Adams", result.getLabel());
        assertEquals("English writer and humorist", result.getDescription());
        assertEquals("Q5", result.getType());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithInvalidId_ThrowsException() {
        Map<String, Object> mockResponse = Map.of(
            "error", Map.of("info", "Entity not found")
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        assertThrows(RuntimeException.class, () -> {
            wikidataService.getEntityById("Q999999");
        });
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithMalformedResponse_ThrowsException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of()));

        assertThrows(RuntimeException.class, () -> {
            wikidataService.getEntityById("Q42");
        });
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithNullResponse_ThrowsException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(null));

        assertThrows(RuntimeException.class, () -> {
            wikidataService.getEntityById("Q42");
        });
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithEmptyResponse_ThrowsException() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of()));

        assertThrows(RuntimeException.class, () -> {
            wikidataService.getEntityById("Q42");
        });
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithMissingLabel_ReturnsNullLabel() {
        Map<String, Object> mockResponse = Map.of(
            "entities", Map.of(
                "Q42", Map.of(
                    "id", "Q42",
                    "descriptions", Map.of("en", Map.of("value", "English writer and humorist")),
                    "claims", Map.of(
                        "P31", List.of(Map.of(
                            "mainsnak", Map.of(
                                "datavalue", Map.of(
                                    "value", Map.of(
                                        "entity-type", "item",
                                        "numeric-id", 5
                                    )
                                )
                            ),
                            "type", "statement"
                        ))
                    )
                )
            )
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        WikidataEntityDTO result = wikidataService.getEntityById("Q42");

        assertNotNull(result);
        assertEquals("Q42", result.getId());
        assertNull(result.getLabel());
        assertEquals("English writer and humorist", result.getDescription());
        assertEquals("Q5", result.getType());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithMissingDescription_ReturnsNullDescription() {
        Map<String, Object> mockResponse = Map.of(
            "entities", Map.of(
                "Q42", Map.of(
                    "id", "Q42",
                    "labels", Map.of("en", Map.of("value", "Douglas Adams")),
                    "claims", Map.of(
                        "P31", List.of(Map.of(
                            "mainsnak", Map.of(
                                "datavalue", Map.of(
                                    "value", Map.of(
                                        "entity-type", "item",
                                        "numeric-id", 5
                                    )
                                )
                            ),
                            "type", "statement"
                        ))
                    )
                )
            )
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        WikidataEntityDTO result = wikidataService.getEntityById("Q42");

        assertNotNull(result);
        assertEquals("Q42", result.getId());
        assertEquals("Douglas Adams", result.getLabel());
        assertNull(result.getDescription());
        assertEquals("Q5", result.getType());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithMultipleTypes_ReturnsFirstType() {
        Map<String, Object> mockResponse = Map.of(
            "entities", Map.of(
                "Q42", Map.of(
                    "id", "Q42",
                    "labels", Map.of("en", Map.of("value", "Douglas Adams")),
                    "descriptions", Map.of("en", Map.of("value", "English writer and humorist")),
                    "claims", Map.of(
                        "P31", List.of(
                            Map.of(
                                "mainsnak", Map.of(
                                    "datavalue", Map.of(
                                        "value", Map.of(
                                            "entity-type", "item",
                                            "numeric-id", 5
                                        )
                                    )
                                ),
                                "type", "statement"
                            ),
                            Map.of(
                                "mainsnak", Map.of(
                                    "datavalue", Map.of(
                                        "value", Map.of(
                                            "entity-type", "item",
                                            "numeric-id", 123
                                        )
                                    )
                                ),
                                "type", "statement"
                            )
                        )
                    )
                )
            )
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        WikidataEntityDTO result = wikidataService.getEntityById("Q42");

        assertNotNull(result);
        assertEquals("Q42", result.getId());
        assertEquals("Douglas Adams", result.getLabel());
        assertEquals("English writer and humorist", result.getDescription());
        assertEquals("Q5", result.getType());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void getEntityById_WithNoTypes_ReturnsNullType() {
        Map<String, Object> mockResponse = Map.of(
            "entities", Map.of(
                "Q42", Map.of(
                    "id", "Q42",
                    "labels", Map.of("en", Map.of("value", "Douglas Adams")),
                    "descriptions", Map.of("en", Map.of("value", "English writer and humorist")),
                    "claims", Map.of()
                )
            )
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(mockResponse));

        WikidataEntityDTO result = wikidataService.getEntityById("Q42");

        assertNotNull(result);
        assertEquals("Q42", result.getId());
        assertEquals("Douglas Adams", result.getLabel());
        assertEquals("English writer and humorist", result.getDescription());
        assertNull(result.getType());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }
} 