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
import java.util.stream.Collectors;

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

    // Cache for property labels and descriptions to avoid repeated API calls
    private final Map<String, Map<String, String>> propertyCache = new ConcurrentHashMap<>();

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
        // Check for empty query
        if (query == null || query.trim().isEmpty()) {
            return createEmptyResponse(page, size);
        }

        waitForRateLimit("searchTopics");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "SWE573 Thread App/1.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Use the MediaWiki API directly instead of SPARQL
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
    @Cacheable(value = "wikidataTopic", key = "#id")
    public WikidataEntityDTO getTopicDetails(String id) {
        String sparqlQuery = 
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX schema: <http://schema.org/>\n" +
            "PREFIX p: <http://www.wikidata.org/prop/>\n" +
            "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n" +
            "\n" +
            "SELECT ?item ?itemLabel ?itemDescription ?propEntity ?property ?propertyLabel ?propertyDescription ?value ?valueLabel\n" +
            "WHERE {\n" +
            "  VALUES ?item { wd:" + id + " }\n" +
            "  ?item ?property ?value .\n" +
            "  \n" +
            "  # Only include direct properties (wdt:) and properly labeled ones\n" +
            "  FILTER(STRSTARTS(STR(?property), STR(wdt:)))\n" +
            "  \n" +
            "  # Extract property entity ID for better labeling\n" +
            "  BIND(REPLACE(STR(?property), \"^http://www.wikidata.org/prop/direct/([P\\\\d]+)$\", \"http://www.wikidata.org/entity/$1\") AS ?propEntity)\n" +
            "  \n" +
            "  # Include labels for everything with fallbacks to multiple languages\n" +
            "  SERVICE wikibase:label { \n" +
            "    bd:serviceParam wikibase:language \"en,en-gb,en-us,de,fr,es,it,nl,ja,zh,ru,pt\" . \n" +
            "    ?item rdfs:label ?itemLabel . \n" +
            "    ?item schema:description ?itemDescription . \n" +
            "    ?propEntity rdfs:label ?propertyLabel . \n" +
            "    ?propEntity schema:description ?propertyDescription . \n" +
            "    ?value rdfs:label ?valueLabel . \n" +
            "  }\n" +
            "}\n" +
            "ORDER BY ?propertyLabel";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/sparql-results+json");
        headers.set(HttpHeaders.USER_AGENT, "SWE573 Thread App/1.0");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Build and encode the URI in one go:
        java.net.URI uri = UriComponentsBuilder
            .fromHttpUrl(WIKIDATA_SPARQL_BASE)
            .queryParam("query", sparqlQuery)
            .build()
            .encode()
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
                String itemUri = (String) itemMap.get("value");
                // Extract only the ID part from the URI
                String id;
                if (itemUri.contains("entity/")) {
                    id = itemUri.substring(itemUri.lastIndexOf("entity/") + 7);
                } else {
                    // Fallback to the old approach
                    id = itemUri.replace("http://www.wikidata.org/entity/", "");
                }
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
                String propUri = (String) propMap.get("value");
                // Extract only the ID part from the URI
                String id;
                if (propUri.contains("entity/")) {
                    id = propUri.substring(propUri.lastIndexOf("entity/") + 7);
                } else {
                    // Fallback to the old approach
                    id = propUri.replace("http://www.wikidata.org/entity/", "");
                }
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

    /**
     * Fetches property information for multiple property IDs in a single API call
     * @param propertyIds List of Wikidata property IDs (e.g., P17, P31)
     * @return Map of property ID to property info (label and description)
     */
    private Map<String, Map<String, String>> batchGetPropertyInfo(List<String> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Filter out invalid IDs and already cached properties
        List<String> idsToFetch = propertyIds.stream()
                .filter(id -> id != null && id.matches("^P\\d+$"))
                .filter(id -> !propertyCache.containsKey(id))
                .distinct()
                .toList();
        
        if (idsToFetch.isEmpty()) {
            // All properties are either invalid or already cached
            return propertyIds.stream()
                    .filter(id -> id != null && id.matches("^P\\d+$"))
                    .collect(Collectors.toMap(
                        id -> id,
                        id -> propertyCache.getOrDefault(id, Map.of("label", "Property", "description", ""))
                    ));
        }
        
        waitForRateLimit("batchGetPropertyInfo");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Join IDs with | for the API call (Wikidata API supports multiple entities in one call)
            String joinedIds = String.join("|", idsToFetch);
            
            String url = UriComponentsBuilder.fromUriString(WIKIDATA_API_BASE)
                    .queryParam("action", "wbgetentities")
                    .queryParam("ids", joinedIds)
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
            Map<String, Map<String, String>> batchResults = new HashMap<>();
            
            if (result != null && result.containsKey("entities")) {
                Map<String, Object> entities = (Map<String, Object>) result.get("entities");
                
                // Process each entity in the response
                for (String propertyId : idsToFetch) {
                    Map<String, String> propertyInfo = new HashMap<>();
                    Map<String, Object> propertyData = (Map<String, Object>) entities.get(propertyId);
                    
                    if (propertyData != null) {
                        // Extract label
                        Map<String, Object> labels = (Map<String, Object>) propertyData.get("labels");
                        if (labels != null && labels.containsKey("en")) {
                            Map<String, Object> enLabel = (Map<String, Object>) labels.get("en");
                            propertyInfo.put("label", (String) enLabel.get("value"));
                        } else {
                            propertyInfo.put("label", "Property");
                        }
                        
                        // Extract description
                        Map<String, Object> descriptions = (Map<String, Object>) propertyData.get("descriptions");
                        if (descriptions != null && descriptions.containsKey("en")) {
                            Map<String, Object> enDesc = (Map<String, Object>) descriptions.get("en");
                            propertyInfo.put("description", (String) enDesc.get("value"));
                        } else {
                            propertyInfo.put("description", "");
                        }
                        
                        // Cache the result
                        propertyCache.put(propertyId, propertyInfo);
                        batchResults.put(propertyId, propertyInfo);
                    } else {
                        // Property not found
                        propertyInfo.put("label", "Property");
                        propertyInfo.put("description", "");
                        batchResults.put(propertyId, propertyInfo);
                    }
                }
            }
            
            // Add already cached properties to the result
            for (String propertyId : propertyIds) {
                if (propertyId != null && propertyId.matches("^P\\d+$") && propertyCache.containsKey(propertyId)) {
                    batchResults.put(propertyId, propertyCache.get(propertyId));
                } else if (!batchResults.containsKey(propertyId)) {
                    batchResults.put(propertyId, Map.of("label", "Property", "description", ""));
                }
            }
            
            return batchResults;
        } catch (Exception e) {
            System.err.println("Error in batch fetching property info: " + e.getMessage());
            // Return default values for all requested properties
            return propertyIds.stream()
                    .collect(Collectors.toMap(
                        id -> id,
                        id -> Map.of("label", "Property", "description", "")
                    ));
        }
    }

    /**
     * Fetches property information from Wikidata and caches it
     * @param propertyId The Wikidata property ID (e.g., P17)
     * @return A map containing label and description for the property
     */
    @Cacheable(value = "propertyInfo", key = "#propertyId")
    private Map<String, String> getPropertyInfo(String propertyId) {
        if (propertyId == null || !propertyId.matches("^P\\d+$")) {
            return Map.of("label", "Property", "description", "");
        }
        
        // Check cache first
        if (propertyCache.containsKey(propertyId)) {
            return propertyCache.get(propertyId);
        }
        
        // Fetch in batch to improve performance
        Map<String, Map<String, String>> batchResult = batchGetPropertyInfo(List.of(propertyId));
        return batchResult.getOrDefault(propertyId, Map.of("label", "Property", "description", ""));
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
        String itemUri = extractValue(firstBinding, "item");
        // Extract only the ID part (Q or P followed by numbers) from the full URI
        String id = null;
        if (itemUri != null) {
            // Handle case where the URI might be in different formats
            if (itemUri.contains("entity/")) {
                id = itemUri.substring(itemUri.lastIndexOf("entity/") + 7);
            } else if (itemUri.matches("^[QP]\\d+$")) {
                // Already in the correct format
                id = itemUri;
            }
        }
        
        entity.setId(id);
        entity.setLabel(extractValue(firstBinding, "itemLabel"));
        entity.setDescription(extractValue(firstBinding, "itemDescription"));
        
        // Ensure proper URL format for Wikidata item
        if (id != null) {
            entity.setUrl("https://www.wikidata.org/wiki/" + id);
        }
        
        // Determine entity type
        String type = extractValue(firstBinding, "type");
        if (type != null) {
            entity.setType(type);
        } else {
            entity.setType("topic"); // Default type
        }

        Map<String, String> properties = new HashMap<>();
        Map<String, String> propertyDescriptions = new HashMap<>();
        
        // First, collect all property IDs to fetch them in batch
        List<String> propertyIdsToFetch = new ArrayList<>();
        Map<String, Map<String, Object>> bindingsByPropertyId = new HashMap<>();
        
        for (Map<String, Object> binding : bindings) {
            String propertyId = extractValue(binding, "property");
            
            // Skip entries with missing data
            if (propertyId == null) {
                continue;
            }
            
            // Extract P-ID from the property URI
            String pId = null;
            if (propertyId.contains("prop/direct/")) {
                pId = propertyId.substring(propertyId.lastIndexOf("/") + 1);
                if (pId.matches("^P\\d+$")) {
                    // Add to the list for batch fetching if it's a valid P-ID
                    propertyIdsToFetch.add(pId);
                    
                    // Store the binding for later processing
                    bindingsByPropertyId.put(pId, binding);
                }
            }
        }
        
        // Fetch all property info in one batch call
        Map<String, Map<String, String>> propertyInfoBatch = batchGetPropertyInfo(propertyIdsToFetch);
        
        // Now process the properties with the batch-fetched info
        for (String pId : bindingsByPropertyId.keySet()) {
            Map<String, Object> binding = bindingsByPropertyId.get(pId);
            String propertyId = extractValue(binding, "property");
            String propertyLabel = extractValue(binding, "propertyLabel");
            String propertyDescription = extractValue(binding, "propertyDescription");
            String value = extractValue(binding, "value");
            String valueLabel = extractValue(binding, "valueLabel");
            
            // Get property info from the batch results
            Map<String, String> propInfo = propertyInfoBatch.getOrDefault(pId, Map.of("label", "Property", "description", ""));
            
            // Create a clean property label
            String cleanPropertyLabel;
            
            // Check if we have a good property label from the query
            if (propertyLabel != null && !propertyLabel.trim().isEmpty() && 
                !propertyLabel.equals(propertyId) && !propertyLabel.equals(pId)) {
                // We have a good human-readable label like "country"
                cleanPropertyLabel = propertyLabel;
                
                // Capitalize first letter if not already capitalized
                if (cleanPropertyLabel.length() > 0 && Character.isLowerCase(cleanPropertyLabel.charAt(0))) {
                    cleanPropertyLabel = Character.toUpperCase(cleanPropertyLabel.charAt(0)) + cleanPropertyLabel.substring(1);
                }
            } else {
                // Try to use the fetched label
                String fetchedLabel = propInfo.get("label");
                
                if (fetchedLabel != null && !fetchedLabel.isEmpty() && !fetchedLabel.equals("Property")) {
                    cleanPropertyLabel = fetchedLabel;
                    // Also update the property description if not already set
                    if (propertyDescription == null || propertyDescription.trim().isEmpty()) {
                        propertyDescription = propInfo.get("description");
                    }
                } else {
                    // We don't have a good label, use formatted P-ID
                    cleanPropertyLabel = "Property";
                }
            }
            
            // Make sure the property label isn't empty
            if (cleanPropertyLabel.trim().isEmpty()) {
                cleanPropertyLabel = "Property";
            }
            
            // Add P-ID in parentheses if available and not already included
            if (!cleanPropertyLabel.contains(pId)) {
                // Remove any existing bracketed text to avoid duplication
                cleanPropertyLabel = cleanPropertyLabel.replaceAll("\\s*\\[[^\\]]*\\]\\s*$", "").trim();
                cleanPropertyLabel += " (" + pId + ")";
            }
            
            // Determine the best display value for this property
            String displayValue;
            
            if (value == null) {
                displayValue = "N/A";
            } else if (valueLabel != null && !valueLabel.trim().isEmpty() && 
                      !valueLabel.equals(value) && !valueLabel.startsWith("http")) {
                // We have a good human-readable label for an entity
                // For entity references, show the label and ID for better reference
                if (value.contains("wikidata.org/entity/")) {
                    String entityId = value.substring(value.lastIndexOf("/") + 1);
                    // Only add the ID if not already part of the label
                    if (!valueLabel.contains(entityId)) {
                        displayValue = valueLabel + " (" + entityId + ")";
                    } else {
                        displayValue = valueLabel;
                    }
                } else {
                    displayValue = valueLabel;
                }
            } else if (value.contains("wikidata.org/entity/")) {
                // It's a Wikidata entity without a label
                String entityId = value.substring(value.lastIndexOf("/") + 1);
                displayValue = entityId;
                
                // Add a link to the entity for easier exploration
                if (!displayValue.contains("http")) {
                    displayValue += " (https://www.wikidata.org/wiki/" + entityId + ")";
                }
            } else if (value.startsWith("http://") || value.startsWith("https://")) {
                // It's a URL - keep as is
                displayValue = value;
            } else if (value.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                // It looks like a date - try to format it nicely
                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(value.substring(0, 10));
                    displayValue = date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                } catch (Exception e) {
                    displayValue = value;
                }
            } else {
                // Regular string value
                displayValue = value;
            }
            
            // Store with the clean property label
            properties.put(cleanPropertyLabel, displayValue);
            
            // Store property description if available
            if (propertyDescription != null && !propertyDescription.trim().isEmpty()) {
                propertyDescriptions.put(cleanPropertyLabel, propertyDescription);
            }
        }
        
        entity.setProperties(properties);
        entity.setPropertyDescriptions(propertyDescriptions);

        return entity;
    }

    private String extractValue(Map<String, Object> binding, String key) {
        Map<String, Object> valueMap = (Map<String, Object>) binding.get(key);
        return valueMap != null ? (String) valueMap.get("value") : null;
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
        String propertyUri = extractValue(firstBinding, "property");
        
        // Extract only the ID part from the URI
        String id = null;
        if (propertyUri != null) {
            if (propertyUri.contains("entity/")) {
                id = propertyUri.substring(propertyUri.lastIndexOf("entity/") + 7);
            } else if (propertyUri.matches("^[QP]\\d+$")) {
                // Already in the correct format
                id = propertyUri;
            }
        }
        
        WikidataPropertyDTO property = new WikidataPropertyDTO();
        property.setId(id);
        property.setLabel(extractValue(firstBinding, "propertyLabel"));
        property.setDescription(extractValue(firstBinding, "propertyDescription"));
        
        // Ensure proper URL format for Wikidata property
        if (id != null) {
            property.setUrl("https://www.wikidata.org/wiki/Property:" + id);
        }
        
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
} 