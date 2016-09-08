package no.deichman.services.search;

import no.deichman.services.entity.EntityService;
import no.deichman.services.entity.EntityType;
import no.deichman.services.uridefaults.BaseURI;
import no.deichman.services.uridefaults.XURI;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.URLEncoder.encode;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

/**
 * Responsibility: perform indexing and searching.
 */
public class SearchServiceImpl implements SearchService {
    public static final Property AGENT = createProperty(BaseURI.ontology("agent"));
    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final String UTF_8 = "UTF-8";
    private final EntityService entityService;
    private final String elasticSearchBaseUrl;
    private ModelToIndexMapper workModelToIndexMapper = new ModelToIndexMapper("work");
    private ModelToIndexMapper personModelToIndexMapper = new ModelToIndexMapper("person");
    private ModelToIndexMapper corporationModelToIndexMapper = new ModelToIndexMapper("corporation");
    private ModelToIndexMapper publicationModelToIndexMapper = new ModelToIndexMapper("publication");

    public SearchServiceImpl(String elasticSearchBaseUrl, EntityService entityService) {
        this.elasticSearchBaseUrl = elasticSearchBaseUrl;
        this.entityService = entityService;
        getIndexUriBuilder();
    }

    @Override
    public final void index(XURI xuri) throws Exception {
        switch (xuri.getTypeAsEntityType()) {
            case WORK:
                doIndexWork(xuri, false);
                break;
            case PERSON:
            case CORPORATION:
                doIndexWorkCreator(xuri, false);
                break;
            case PUBLICATION:
                doIndexPublication(xuri);
                break;
            default:
                doIndex(xuri);
        }
    }

    @Override
    public final Response searchPersonWithJson(String json) {
        return searchWithJson(json, getPersonSearchUriBuilder());
    }

    @Override
    public final Response searchWorkWithJson(String json, MultivaluedMap<String, String> queryParams) {
        return searchWithJson(json, getWorkSearchUriBuilder(queryParams));
    }

    @Override
    public final Response searchPublicationWithJson(String json) {
        return searchWithJson(json, getPublicationSearchUriBuilder());
    }

    @Override
    public final Response searchInstrument(String query) {
        return doSearch(query, getInstrumentSearchUriBuilder());
    }

    @Override
    public final Response searchCompositionType(String query) {
        return doSearch(query, getCompositionTypeSearchUriBuilder());
    }

    @Override
    public final Response searchEvent(String query) {
        return doSearch(query, getEventSearchUriBuilder());
    }


    @Override
    public final Response clearIndex() {
        try (CloseableHttpClient httpclient = createDefault()) {
            URI uri = getIndexUriBuilder().setPath("/search").build();

            try (CloseableHttpResponse getExistingIndex = httpclient.execute(new HttpGet(uri))) {
                if (getExistingIndex.getStatusLine().getStatusCode() == HTTP_OK) {
                    try (CloseableHttpResponse delete = httpclient.execute(new HttpDelete(uri))) {
                        int statusCode = delete.getStatusLine().getStatusCode();
                        LOG.info("Delete index request returned status " + statusCode);
                        if (statusCode != HTTP_OK) {
                            throw new ServerErrorException("Failed to delete elasticsearch index", HTTP_INTERNAL_ERROR);
                        }
                    }
                }
            }
            HttpPut createIndexRequest = new HttpPut(uri);
            createIndexRequest.setEntity(new InputStreamEntity(getClass().getResourceAsStream("/search_index.json"), ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse create = httpclient.execute(createIndexRequest)) {
                int statusCode = create.getStatusLine().getStatusCode();
                LOG.info("Create index request returned status " + statusCode);
                if (statusCode != HTTP_OK) {
                    throw new ServerErrorException("Failed to create elasticsearch index", HTTP_INTERNAL_ERROR);
                }
            }
            putIndexMapping(httpclient, "work");
            putIndexMapping(httpclient, "person");
            putIndexMapping(httpclient, "serial");
            putIndexMapping(httpclient, "corporation");
            putIndexMapping(httpclient, "place");
            putIndexMapping(httpclient, "subject");
            putIndexMapping(httpclient, "genre");
            putIndexMapping(httpclient, "publication");
            putIndexMapping(httpclient, "instrument");
            putIndexMapping(httpclient, "compositiontype");

            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private void putIndexMapping(CloseableHttpClient httpclient, String type) throws URISyntaxException, IOException {
        URI workIndexUri = getIndexUriBuilder().setPath("/search/_mapping/" + type).build();
        HttpPut putWorkMappingRequest = new HttpPut(workIndexUri);
        putWorkMappingRequest.setEntity(new InputStreamEntity(getClass().getResourceAsStream("/" + type + "_mapping.json"), ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse create = httpclient.execute(putWorkMappingRequest)) {
            int statusCode = create.getStatusLine().getStatusCode();
            LOG.info("Create mapping request for " + type + " returned status " + statusCode);
            if (statusCode != HTTP_OK) {
                throw new ServerErrorException("Failed to create elasticsearch mapping for " + type, HTTP_INTERNAL_ERROR);
            }
        }
    }

    private Response searchWithJson(String body, URIBuilder searchUriBuilder) {
        try {
            HttpPost httpPost = new HttpPost(searchUriBuilder.build());
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-type", "application/json");
            return executeHttpRequest(httpPost);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private Response executeHttpRequest(HttpRequestBase httpRequestBase) throws IOException {
        try (CloseableHttpClient httpclient = createDefault();
             CloseableHttpResponse response = httpclient.execute(httpRequestBase)) {
            HttpEntity responseEntity = response.getEntity();
            String jsonContent = IOUtils.toString(responseEntity.getContent());
            Response.ResponseBuilder responseBuilder = Response.ok(jsonContent);
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                responseBuilder = responseBuilder.header(header.getName(), header.getValue());
            }
            return responseBuilder.build();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public final Response searchWork(String query) {
        return doSearch(query, getWorkSearchUriBuilder(null));
    }

    @Override
    public final Response searchPerson(String query) {
        return doSearch(query, getPersonSearchUriBuilder());
    }

    @Override
    public final Response searchPlace(String query) {
        return doSearch(query, getPlaceUriBuilder());
    }

    @Override
    public final Response searchCorporation(String query) {
        return doSearch(query, getCorporationSearchUriBuilder());
    }

    @Override
    public final Response searchSerial(String query) {
        return doSearch(query, getSerialSearchUriBuilder());
    }

    @Override
    public final Response searchSubject(String query) {
        return doSearch(query, getSubjectSearchUriBuilder());
    }

    @Override
    public final Response searchGenre(String query) {
        return doSearch(query, getGenreSearchUriBuilder());
    }

    @Override
    public final Response searchPublication(String query) {
        return doSearch(query, getPublicationSearchUriBuilder());
    }

    @Override
    public final void delete(XURI xuri) {
        try (CloseableHttpClient httpclient = createDefault()) {
            HttpDelete httpDelete = new HttpDelete(getIndexUriBuilder()
                    .setPath(format("/search/%s/%s", xuri.getType(), encode(xuri.getUri(), UTF_8)))
                    .build());
            try (CloseableHttpResponse putResponse = httpclient.execute(httpDelete)) {
                LOG.debug(putResponse.getStatusLine().toString());
            }
        } catch (Exception e) {
            LOG.error(format("Failed to delete %s in elasticsearch", xuri.getUri()), e);
            throw new ServerErrorException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private void doIndexPublication(XURI pubUri) throws Exception {
        Model pubModel = entityService.retrieveById(pubUri);
        Property publicationOfProperty = ResourceFactory.createProperty(BaseURI.ontology("publicationOf"));
        if (pubModel.getProperty(null, publicationOfProperty) != null) {
            String workUri = pubModel.getProperty(ResourceFactory.createResource(pubUri.toString()), publicationOfProperty).getObject().toString();
            XURI workXURI = new XURI(workUri);
            pubModel = entityService.retrieveWorkWithLinkedResources(workXURI);
        }
        indexDocument(pubUri, publicationModelToIndexMapper.createIndexDocument(pubModel, pubUri));
    }

    private void doIndexWork(XURI xuri, boolean indexedPerson) throws Exception {

        Model workModelWithLinkedResources = entityService.retrieveWorkWithLinkedResources(xuri);
        indexDocument(xuri, workModelToIndexMapper.createIndexDocument(workModelWithLinkedResources, xuri));

        if (!indexedPerson) {
            for (Statement stmt : workModelWithLinkedResources.listStatements().toList()) {
                if (stmt.getPredicate().equals(AGENT)) {
                    XURI creatorXuri = new XURI(stmt.getObject().asNode().getURI());
                    doIndexWorkCreatorOnly(creatorXuri);
                }
            }
        }

        // Index all publications belonging to work
        // TODO instead of iterating over all subjects, find only subjects of triples with publicationOf as predicate
        ResIterator subjectIterator = workModelWithLinkedResources.listSubjects();
        while (subjectIterator.hasNext()) {
            Resource subj = subjectIterator.next();
            if (subj.isAnon()) {
                continue;
            }
            if (subj.toString().contains("publication")) {
                XURI pubUri = new XURI(subj.toString());
                indexDocument(pubUri, publicationModelToIndexMapper.createIndexDocument(workModelWithLinkedResources, pubUri));

            }
        }

    }

    private void doIndexWorkCreator(XURI creatorUri, boolean indexedWork) throws Exception {
        Model works = entityService.retrieveWorksByCreator(creatorUri);
        if (!indexedWork) {
            ResIterator subjectIterator = works.listSubjects();
            while (subjectIterator.hasNext()) {
                Resource subj = subjectIterator.next();
                if (subj.isAnon()) {
                    continue;
                }
                XURI workUri = new XURI(subj.toString());
                if (!workUri.getUri().equals(creatorUri.getUri())) {
                    doIndexWorkOnly(workUri);
                }
            }
        }
        switch (creatorUri.getTypeAsEntityType()) {
            case PERSON:
                indexDocument(creatorUri, personModelToIndexMapper
                        .createIndexDocument(entityService.retrievePersonWithLinkedResources(creatorUri).add(works), creatorUri));
                break;
            case CORPORATION:
                indexDocument(creatorUri, corporationModelToIndexMapper
                        .createIndexDocument(entityService.retrieveCorporationWithLinkedResources(creatorUri).add(works), creatorUri));
                break;
            default:
                throw new RuntimeException(format(
                        "Tried to index work creator of type %1$s. Should be %2$s or %3$s",
                        creatorUri.getTypeAsEntityType(), EntityType.PERSON, EntityType.CORPORATION
                ));
        }
    }

    private void doIndex(XURI xuri) throws Exception {
        Model indexModel = entityService.retrieveById(xuri);
        indexDocument(xuri, new ModelToIndexMapper(xuri.getTypeAsEntityType().getPath()).createIndexDocument(indexModel, xuri));
    }

    private void doIndexWorkOnly(XURI xuri) throws Exception {
        doIndexWork(xuri, true);
    }

    private void indexDocument(XURI xuri, String document) {
        try (CloseableHttpClient httpclient = createDefault()) {
            HttpPut httpPut = new HttpPut(getIndexUriBuilder()
                    .setPath(format("/search/%s/%s", xuri.getType(), encode(xuri.getUri(), UTF_8))) // TODO drop urlencoded ID, and define _id in mapping from field uri
                    .build());
            httpPut.setEntity(new StringEntity(document, Charset.forName(UTF_8)));
            httpPut.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.withCharset(UTF_8).toString());
            try (CloseableHttpResponse putResponse = httpclient.execute(httpPut)) {
                LOG.debug(putResponse.getStatusLine().toString());
            }
        } catch (Exception e) {
            LOG.error(format("Failed to index %s in elasticsearch", xuri.getUri()), e);
            throw new ServerErrorException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private Response doSearch(String query, URIBuilder searchUriBuilder) {
        try {
            HttpGet httpGet = new HttpGet(searchUriBuilder.setParameter("q", query).build());
            return executeHttpRequest(httpGet);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private void doIndexWorkCreatorOnly(XURI xuri) throws Exception {
        doIndexWorkCreator(xuri, true);
    }

    private URIBuilder getIndexUriBuilder() {
        try {
            return new URIBuilder(this.elasticSearchBaseUrl);
        } catch (URISyntaxException e) {
            LOG.error("Failed to create uri builder for elasticsearch");
            throw new RuntimeException(e);
        }
    }

    private URIBuilder getWorkSearchUriBuilder(MultivaluedMap<String, String> queryParams) {
        URIBuilder uriBuilder = getIndexUriBuilder().setPath("/search/work/_search");
        if (queryParams != null && !queryParams.isEmpty()) {
            List<NameValuePair> nvpList = new ArrayList<>(queryParams.size());
            queryParams.forEach((key, values) -> {
                values.forEach(value -> {
                    nvpList.add(new BasicNameValuePair(key, value));
                });
            });
            uriBuilder.setParameters(nvpList);
        }
        return uriBuilder;
    }

    private URIBuilder getPersonSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/person/_search");
    }

    public final URIBuilder getPlaceUriBuilder() {
        return getIndexUriBuilder().setPath("/search/place/_search");
    }

    public final URIBuilder getCorporationSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/corporation/_search");
    }

    public final URIBuilder getSerialSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/serial/_search");
    }

    public final URIBuilder getSubjectSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/subject/_search");
    }

    public final URIBuilder getGenreSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/genre/_search");
    }

    public final URIBuilder getPublicationSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/publication/_search");
    }

    public final URIBuilder getInstrumentSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/instrument/_search");
    }

    public final URIBuilder getCompositionTypeSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/compositiontype/_search");
    }

    private URIBuilder getEventSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/event/_search");    }
}
