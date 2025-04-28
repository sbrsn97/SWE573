package com.swe573.services.impl;

import com.swe573.dto.WikidataEntityDTO;
import com.swe573.dto.WikidataPropertyDTO;
import com.swe573.dto.PaginatedResponse;
import com.swe573.services.WikidataService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WikidataServiceImpl implements WikidataService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String WIKIDATA_API_BASE = "https://www.wikidata.org/w/api.php";
    private static final String WIKIDATA_SPARQL_BASE = "https://query.wikidata.org/sparql";
    
    // Rate limiting configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // Wikidata's rate limit
    private static final long MIN_INTERVAL_BETWEEN_REQUESTS = TimeUnit.MINUTES.toMillis(1) / MAX_REQUESTS_PER_MINUTE;
    private final AtomicLong lastRequestTime = new AtomicLong(0);
    private final Map<String, Long> lastRequestTimesByEndpoint = new ConcurrentHashMap<>();

    private void waitForRateLimit(String endpoint) {
        long currentTime = System.currentTimeMillis();
        long lastRequest = lastRequestTimesByEndpoint.getOrDefault(endpoint, 0L);
        long timeSinceLastRequest = currentTime - lastRequest;
        
        if (timeSinceLastRequest < MIN_INTERVAL_BETWEEN_REQUESTS) {
            try {
                Thread.sleep(MIN_INTERVAL_BETWEEN_REQUESTS - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        lastRequestTimesByEndpoint.put(endpoint, System.currentTimeMillis());
    }

    @Override
    @Cacheable(value = "entities", key = "#page + '-' + #size")
    public PaginatedResponse<WikidataEntityDTO> getAllEntities(int page, int size) {
        // Query to get entities with the most sitelinks (most popular)
        String sparqlQuery = 
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX schema: <http://schema.org/>\n" +
            "\n" +
            "SELECT ?item ?itemLabel ?itemDescription WHERE {\n" +
            "  ?item wikibase:sitelinks ?count .\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }\n" +
            "}\n" +
            "ORDER BY DESC(?count)\n" +
            "LIMIT " + size + "\n" +
            "OFFSET " + (page * size);

        return executeEntityQuery(sparqlQuery, page, size);
    }

    @Override
    @Cacheable(value = "wikidataEntities", key = "'search-' + #query + '-' + #page + '-' + #size", unless = "#result == null || #result.items.isEmpty()")
    public PaginatedResponse<WikidataEntityDTO> searchEntities(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            return createEmptyResponse(page, size);
        }

        waitForRateLimit("searchEntities");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "SWE573 Thread App/1.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromUriString(WIKIDATA_API_BASE)
                .queryParam("action", "wbsearchentities")
                .queryParam("search", query)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .queryParam("limit", size)
                .queryParam("offset", page * size)
                .build()
                .toUriString();

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> result = response.getBody();
        return parseApiSearchResponse(result, page, size);
    }

    @Override
    @Cacheable(value = "wikidataEntity", key = "#id")
    public WikidataEntityDTO getEntityById(String id) {
        waitForRateLimit("getEntityById");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromUriString(WIKIDATA_API_BASE)
                .queryParam("action", "wbgetentities")
                .queryParam("ids", id)
                .queryParam("languages", "en")
                .queryParam("format", "json")
                .build()
                .toUriString();

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> result = response.getBody();
        return parseApiEntityResponse(result, id);
    }

    @Override
    @Cacheable(value = "properties", key = "#page + '-' + #size")
    public PaginatedResponse<WikidataPropertyDTO> getAllProperties(int page, int size) {
        String sparqlQuery = 
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX schema: <http://schema.org/>\n" +
            "\n" +
            "SELECT ?property ?propertyLabel ?propertyDescription\n" +
            "WHERE {\n" +
            "  ?property a wikibase:Property .\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }\n" +
            "}\n" +
            "ORDER BY ?propertyLabel\n" +
            "LIMIT " + size + "\n" +
            "OFFSET " + (page * size);

        return executePropertyQuery(sparqlQuery, page, size);
    }

    @Override
    @Cacheable(value = "wikidataProperties", key = "'search-' + #query + '-' + #page + '-' + #size", unless = "#result == null || #result.items.isEmpty()")
    public PaginatedResponse<WikidataPropertyDTO> searchProperties(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            return createEmptyPropertyResponse(page, size);
        }

        waitForRateLimit("searchProperties");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromUriString(WIKIDATA_API_BASE)
                .queryParam("action", "wbsearchentities")
                .queryParam("search", query)
                .queryParam("language", "en")
                .queryParam("type", "property")
                .queryParam("format", "json")
                .queryParam("limit", size)
                .queryParam("offset", page * size)
                .build()
                .toUriString();

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> result = response.getBody();
        return parseApiPropertySearchResponse(result, page, size);
    }

    @Override
    @Cacheable(value = "wikidataProperty", key = "#id")
    public WikidataPropertyDTO getPropertyById(String id) {
        waitForRateLimit("getPropertyById");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromUriString(WIKIDATA_API_BASE)
                .queryParam("action", "wbgetentities")
                .queryParam("ids", id)
                .queryParam("languages", "en")
                .queryParam("format", "json")
                .build()
                .toUriString();

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> result = response.getBody();
        return parseApiPropertyResponse(result, id);
    }

    @Override
    @Cacheable(value = "wikidataTopics", key = "'search-' + #query + '-' + #page + '-' + #size")
    public PaginatedResponse<WikidataEntityDTO> searchTopics(String query, int page, int size) {
        // Use the MediaWiki API service for more reliable text searches
        String escapedQuery = query.replace("\"", "\\\"");
        String sparqlQuery = 
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX mwapi: <https://www.mediawiki.org/ontology#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX schema: <http://schema.org/>\n" +
            "\n" +
            "SELECT ?item ?itemLabel ?itemDescription WHERE {\n" +
            "  SERVICE wikibase:mwapi {\n" +
            "    bd:serviceParam wikibase:api \"EntitySearch\" .\n" +
            "    bd:serviceParam wikibase:endpoint \"www.wikidata.org\" .\n" +
            "    bd:serviceParam mwapi:search \"" + escapedQuery + "\" .\n" +
            "    bd:serviceParam mwapi:language \"en\" .\n" +
            "    ?item wikibase:apiOutputItem mwapi:item .\n" +
            "  }\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }\n" +
            "}\n" +
            "LIMIT " + size + "\n" +
            "OFFSET " + (page * size);

        return executeEntityQuery(sparqlQuery, page, size);
    }

    @Override
    @Cacheable(value = "wikidataTopic", key = "#id")
    public WikidataEntityDTO getTopicDetails(String id) {
        String sparqlQuery = 
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX schema: <http://schema.org/>\n" +
            "\n" +
            "SELECT ?item ?itemLabel ?itemDescription ?property ?propertyLabel ?value ?valueLabel\n" +
            "WHERE {\n" +
            "  VALUES ?item { wd:" + id + " }\n" +
            "  ?item ?property ?value .\n" +
            "  FILTER(?property != wdt:P31 && ?property != wdt:P279) .\n" +
            "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }\n" +
            "}";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/sparql-results+json");
        headers.set(HttpHeaders.USER_AGENT, "SWE573 Thread App/1.0");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Build and encode the URI in one go:
        java.net.URI uri = UriComponentsBuilder
            .fromHttpUrl(WIKIDATA_SPARQL_BASE)        // "https://query.wikidata.org/sparql"
            .queryParam("query", sparqlQuery)         // your raw SPARQL string
            .build()                                  // bind into a URI template
            .encode()                                 // percent-encode path & query appropriately
            .toUri();

        ResponseEntity<Map> response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        Map<String, Object> result = response.getBody();
        return parseEntityResult(result);
    }

    private PaginatedResponse<WikidataEntityDTO> createEmptyResponse(int page, int size) {
        PaginatedResponse<WikidataEntityDTO> emptyResponse = new PaginatedResponse<>();
        emptyResponse.setItems(new ArrayList<>());
        emptyResponse.setCurrentPage(page);
        emptyResponse.setPageSize(size);
        emptyResponse.setTotalItems(0);
        emptyResponse.setTotalPages(0);
        return emptyResponse;
    }

    private PaginatedResponse<WikidataPropertyDTO> createEmptyPropertyResponse(int page, int size) {
        PaginatedResponse<WikidataPropertyDTO> emptyResponse = new PaginatedResponse<>();
        emptyResponse.setItems(new ArrayList<>());
        emptyResponse.setCurrentPage(page);
        emptyResponse.setPageSize(size);
        emptyResponse.setTotalItems(0);
        emptyResponse.setTotalPages(0);
        return emptyResponse;
    }

    private PaginatedResponse<WikidataEntityDTO> parseApiSearchResponse(Map<String, Object> result, int page, int size) {
        List<WikidataEntityDTO> entities = new ArrayList<>();
        
        if (result != null && result.containsKey("search")) {
            List<Map<String, Object>> searchResults = (List<Map<String, Object>>) result.get("search");
            for (Map<String, Object> item : searchResults) {
                WikidataEntityDTO entity = new WikidataEntityDTO();
                entity.setId((String) item.get("id"));
                entity.setLabel((String) item.get("label"));
                entity.setDescription((String) item.get("description"));
                entities.add(entity);
            }
        }

        PaginatedResponse<WikidataEntityDTO> response = new PaginatedResponse<>();
        response.setItems(entities);
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setTotalItems(entities.size());
        response.setTotalPages((int) Math.ceil((double) entities.size() / size));
        return response;
    }

    private WikidataEntityDTO parseApiEntityResponse(Map<String, Object> result, String id) {
        if (result == null) {
            throw new RuntimeException("Null response from Wikidata API");
        }

        if (!result.containsKey("entities")) {
            throw new RuntimeException("Malformed response from Wikidata API");
        }

        Map<String, Object> entities = (Map<String, Object>) result.get("entities");
        Map<String, Object> entityData = (Map<String, Object>) entities.get(id);
        
        if (entityData == null) {
            throw new RuntimeException("Entity not found: " + id);
        }

        WikidataEntityDTO entity = new WikidataEntityDTO();
        entity.setId(id);
        
        Map<String, Object> labels = (Map<String, Object>) entityData.get("labels");
        if (labels != null && labels.containsKey("en")) {
            Map<String, Object> enLabel = (Map<String, Object>) labels.get("en");
            entity.setLabel((String) enLabel.get("value"));
        }

        Map<String, Object> descriptions = (Map<String, Object>) entityData.get("descriptions");
        if (descriptions != null && descriptions.containsKey("en")) {
            Map<String, Object> enDesc = (Map<String, Object>) descriptions.get("en");
            entity.setDescription((String) enDesc.get("value"));
        }

        Map<String, Object> claims = (Map<String, Object>) entityData.get("claims");
        if (claims != null && claims.containsKey("P31")) {
            List<Map<String, Object>> typeClaims = (List<Map<String, Object>>) claims.get("P31");
            if (!typeClaims.isEmpty()) {
                Map<String, Object> firstType = typeClaims.get(0);
                Map<String, Object> mainsnak = (Map<String, Object>) firstType.get("mainsnak");
                if (mainsnak != null && mainsnak.containsKey("datavalue")) {
                    Map<String, Object> datavalue = (Map<String, Object>) mainsnak.get("datavalue");
                    if (datavalue != null && datavalue.containsKey("value")) {
                        Map<String, Object> value = (Map<String, Object>) datavalue.get("value");
                        if (value != null && value.containsKey("numeric-id")) {
                            Integer numericId = (Integer) value.get("numeric-id");
                            entity.setType("Q" + numericId);
                        }
                    }
                }
            }
        }

        return entity;
    }

    private PaginatedResponse<WikidataPropertyDTO> parseApiPropertySearchResponse(Map<String, Object> result, int page, int size) {
        List<WikidataPropertyDTO> properties = new ArrayList<>();
        
        if (result != null && result.containsKey("search")) {
            List<Map<String, Object>> searchResults = (List<Map<String, Object>>) result.get("search");
            for (Map<String, Object> item : searchResults) {
                WikidataPropertyDTO property = new WikidataPropertyDTO();
                property.setId((String) item.get("id"));
                property.setLabel((String) item.get("label"));
                property.setDescription((String) item.get("description"));
                properties.add(property);
            }
        }

        PaginatedResponse<WikidataPropertyDTO> response = new PaginatedResponse<>();
        response.setItems(properties);
        response.setCurrentPage(page);
        response.setPageSize(size);
        response.setTotalItems(properties.size());
        response.setTotalPages((int) Math.ceil((double) properties.size() / size));
        return response;
    }

    private WikidataPropertyDTO parseApiPropertyResponse(Map<String, Object> result, String id) {
        if (result == null || !result.containsKey("entities")) {
            return null;
        }

        Map<String, Object> entities = (Map<String, Object>) result.get("entities");
        Map<String, Object> propertyData = (Map<String, Object>) entities.get(id);
        
        if (propertyData == null) {
            return null;
        }

        WikidataPropertyDTO property = new WikidataPropertyDTO();
        property.setId(id);
        
        Map<String, Object> labels = (Map<String, Object>) propertyData.get("labels");
        if (labels != null && labels.containsKey("en")) {
            Map<String, Object> enLabel = (Map<String, Object>) labels.get("en");
            property.setLabel((String) enLabel.get("value"));
        }

        Map<String, Object> descriptions = (Map<String, Object>) propertyData.get("descriptions");
        if (descriptions != null && descriptions.containsKey("en")) {
            Map<String, Object> enDesc = (Map<String, Object>) descriptions.get("en");
            property.setDescription((String) enDesc.get("value"));
        }

        return property;
    }

    private PaginatedResponse<WikidataEntityDTO> executeEntityQuery(String sparqlQuery, int page, int size) {
        waitForRateLimit("executeEntityQuery");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/sparql-results+json");
        headers.set(HttpHeaders.USER_AGENT, "SWE573 Thread App/1.0");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Build and encode the URI in one go:
        java.net.URI uri = UriComponentsBuilder
            .fromHttpUrl(WIKIDATA_SPARQL_BASE)        // "https://query.wikidata.org/sparql"
            .queryParam("query", sparqlQuery)         // your raw SPARQL string
            .build()                                  // bind into a URI template
            .encode()                                 // percent-encode path & query appropriately
            .toUri();

        ResponseEntity<Map> response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        Map<String, Object> result = response.getBody();
        if (result == null || !result.containsKey("results") || 
            ((Map<String, Object>) result.get("results")).get("bindings") == null) {
            PaginatedResponse<WikidataEntityDTO> emptyResponse = new PaginatedResponse<>();
            emptyResponse.setItems(new ArrayList<>());
            emptyResponse.setCurrentPage(page);
            emptyResponse.setPageSize(size);
            emptyResponse.setTotalItems(0);
            emptyResponse.setTotalPages(0);
            return emptyResponse;
        }

        List<Map<String, Object>> bindings = (List<Map<String, Object>>) ((Map<String, Object>) result.get("results")).get("bindings");
        List<WikidataEntityDTO> entities = new ArrayList<>();

        for (Map<String, Object> binding : bindings) {
            WikidataEntityDTO entity = new WikidataEntityDTO();
            Map<String, Object> itemMap = (Map<String, Object>) binding.get("item");
            Map<String, Object> labelMap = (Map<String, Object>) binding.get("itemLabel");
            Map<String, Object> descMap = (Map<String, Object>) binding.get("itemDescription");

            if (itemMap != null && itemMap.containsKey("value")) {
                String id = ((String) itemMap.get("value")).replace("http://www.wikidata.org/entity/", "");
                entity.setId(id);
            }
            if (labelMap != null && labelMap.containsKey("value")) {
                entity.setLabel((String) labelMap.get("value"));
            }
            if (descMap != null && descMap.containsKey("value")) {
                entity.setDescription((String) descMap.get("value"));
            }
            entities.add(entity);
        }

        PaginatedResponse<WikidataEntityDTO> paginatedResponse = new PaginatedResponse<>();
        paginatedResponse.setItems(entities);
        paginatedResponse.setCurrentPage(page);
        paginatedResponse.setPageSize(size);
        paginatedResponse.setTotalItems(entities.size());
        paginatedResponse.setTotalPages((int) Math.ceil((double) entities.size() / size));

        return paginatedResponse;
    }

    private PaginatedResponse<WikidataPropertyDTO> executePropertyQuery(String sparqlQuery, int page, int size) {
        waitForRateLimit("executePropertyQuery");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/sparql-results+json");
        headers.set(HttpHeaders.USER_AGENT, "SWE573 Thread App/1.0");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Build and encode the URI in one go:
        java.net.URI uri = UriComponentsBuilder
            .fromHttpUrl(WIKIDATA_SPARQL_BASE)        // "https://query.wikidata.org/sparql"
            .queryParam("query", sparqlQuery)         // your raw SPARQL string
            .build()                                  // bind into a URI template
            .encode()                                 // percent-encode path & query appropriately
            .toUri();

        ResponseEntity<Map> response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            requestEntity,
            Map.class
        );

        Map<String, Object> result = response.getBody();
        if (result == null || !result.containsKey("results") || 
            ((Map<String, Object>) result.get("results")).get("bindings") == null) {
            PaginatedResponse<WikidataPropertyDTO> emptyResponse = new PaginatedResponse<>();
            emptyResponse.setItems(new ArrayList<>());
            emptyResponse.setCurrentPage(page);
            emptyResponse.setPageSize(size);
            emptyResponse.setTotalItems(0);
            emptyResponse.setTotalPages(0);
            return emptyResponse;
        }

        List<Map<String, Object>> bindings = (List<Map<String, Object>>) ((Map<String, Object>) result.get("results")).get("bindings");
        List<WikidataPropertyDTO> properties = new ArrayList<>();

        for (Map<String, Object> binding : bindings) {
            WikidataPropertyDTO property = new WikidataPropertyDTO();
            Map<String, Object> propMap = (Map<String, Object>) binding.get("property");
            Map<String, Object> labelMap = (Map<String, Object>) binding.get("propertyLabel");
            Map<String, Object> descMap = (Map<String, Object>) binding.get("propertyDescription");

            if (propMap != null && propMap.containsKey("value")) {
                String id = ((String) propMap.get("value")).replace("http://www.wikidata.org/entity/", "");
                property.setId(id);
            }
            if (labelMap != null && labelMap.containsKey("value")) {
                property.setLabel((String) labelMap.get("value"));
            }
            if (descMap != null && descMap.containsKey("value")) {
                property.setDescription((String) descMap.get("value"));
            }
            properties.add(property);
        }

        PaginatedResponse<WikidataPropertyDTO> paginatedResponse = new PaginatedResponse<>();
        paginatedResponse.setItems(properties);
        paginatedResponse.setCurrentPage(page);
        paginatedResponse.setPageSize(size);
        paginatedResponse.setTotalItems(properties.size());
        paginatedResponse.setTotalPages((int) Math.ceil((double) properties.size() / size));

        return paginatedResponse;
    }

    private WikidataEntityDTO parseEntityResult(Map<String, Object> result) {
        if (result == null || !result.containsKey("results")) {
            return null;
        }
        
        Map<String, Object> resultsMap = (Map<String, Object>) result.get("results");
        if (resultsMap == null || !resultsMap.containsKey("bindings")) {
            return null;
        }
        
        List<Map<String, Object>> bindings = (List<Map<String, Object>>) resultsMap.get("bindings");
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        Map<String, Object> firstBinding = bindings.get(0);
        WikidataEntityDTO entity = new WikidataEntityDTO();
        String id = extractValue(firstBinding, "item");
        entity.setId(id);
        entity.setLabel(extractValue(firstBinding, "itemLabel"));
        entity.setDescription(extractValue(firstBinding, "itemDescription"));
        entity.setUrl("https://www.wikidata.org/wiki/" + id);
        
        // Determine entity type
        String type = extractValue(firstBinding, "type");
        if (type != null) {
            entity.setType(type);
        } else {
            entity.setType("topic"); // Default type
        }

        Map<String, String> properties = new HashMap<>();
        for (Map<String, Object> binding : bindings) {
            String propertyId = extractValue(binding, "property");
            String propertyLabel = extractValue(binding, "propertyLabel");
            String valueLabel = extractValue(binding, "valueLabel");
            if (propertyId != null && propertyLabel != null && valueLabel != null) {
                properties.put(propertyLabel, valueLabel);
            }
        }
        entity.setProperties(properties);

        return entity;
    }

    private WikidataPropertyDTO parsePropertyResult(Map<String, Object> result) {
        if (result == null || !result.containsKey("results")) {
            return null;
        }
        
        Map<String, Object> resultsMap = (Map<String, Object>) result.get("results");
        if (resultsMap == null || !resultsMap.containsKey("bindings")) {
            return null;
        }
        
        List<Map<String, Object>> bindings = (List<Map<String, Object>>) resultsMap.get("bindings");
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        Map<String, Object> firstBinding = bindings.get(0);
        String id = extractValue(firstBinding, "property");
        WikidataPropertyDTO property = new WikidataPropertyDTO();
        property.setId(id);
        property.setLabel(extractValue(firstBinding, "propertyLabel"));
        property.setDescription(extractValue(firstBinding, "propertyDescription"));
        property.setUrl("https://www.wikidata.org/wiki/Property:" + id);
        
        // Try to determine value type and example
        String valueType = extractValue(firstBinding, "valueType");
        String exampleValue = extractValue(firstBinding, "exampleValue");
        if (valueType != null) {
            property.setValueType(valueType);
        }
        if (exampleValue != null) {
            property.setExampleValue(exampleValue);
        }

        return property;
    }

    private String extractValue(Map<String, Object> binding, String key) {
        Map<String, Object> valueMap = (Map<String, Object>) binding.get(key);
        return valueMap != null ? (String) valueMap.get("value") : null;
    }
} 