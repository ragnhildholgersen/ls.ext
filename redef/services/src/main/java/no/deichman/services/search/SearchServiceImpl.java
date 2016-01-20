package no.deichman.services.search;

import no.deichman.services.entity.EntityService;
import no.deichman.services.entity.EntityType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import riotcmd.json;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.URLEncoder.encode;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static no.deichman.services.search.ModelToIndexMapper.ModelToIndexMapperBuilder.modelToIndexMapperBuilder;
import static no.deichman.services.search.PersonModelToIndexMapper.getPersonModelToIndexMapper;
import static no.deichman.services.search.WorkModelToIndexMapper.getworksModelToIndexMapper;
import static no.deichman.services.uridefaults.BaseURI.remote;
import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Responsibility: perform indexing and searching.
 */
public class SearchServiceImpl implements SearchService {

    public static final String WORK_INDEX_TYPE = "work";
    public static final String PERSON_INDEX_TYPE = "person";
    public static final String PLACE_OF_PUBLICATION_TYPE = "placeOfPublication";
    public static final Property CREATOR = createProperty(remote().ontology("creator"));
    private static final Logger LOG = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final String UTF_8 = "UTF-8";
    private static final String WORK_MODEL_TO_INDEX_DOCUMENT_QUERY = format("PREFIX  : <%1$s> \n"
            + "select distinct ?work ?workTitle ?workYear ?creatorName ?creator ?birth ?death\n"
            + "where {\n"
            + "    ?work a :Work ;\n"
            + "             :mainTitle ?workTitle .\n"
            + "    optional { ?work :publicationYear ?workYear. }\n"
            + "    optional { \n"
            + "             ?work :creator ?creator .\n"
            + "             ?creator a :Person ;\n"
            + "                      :name ?creatorName.\n"
            + "             optional {?creator :birthYear ?birth.}\n"
            + "             optional {?creator :deathYear ?death.}\n"
            + "    }\n"
            + "}\n", remote().ontology());
    private static final String PERSON_MODEL_TO_INDEX_DOCUMENT_QUERY = format(""
            + "PREFIX  : <%1$s> \n"
            + "PREFIX  rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX  duo:<http://data.deichman.no/utility#>\n"
            + "select distinct ?person ?personName ?birth ?death ?nationalityLabel ?work ?workTitle ?workYear\n"
            + "where {\n"
            + "    ?person a :Person ;\n"
            + "             :name ?personName .\n"
            + "    optional {?person :birthYear ?birth.}\n"
            + "    optional {?person :deathYear ?death.}\n"
            + "    optional {?person :nationality ?nationality. \n"
            + "              ?nationality a duo:Nationality; \n"
            + "                           rdfs:label ?nationalityLabel. "
            + "    } \n"
            + "    optional {?work :creator ?person ;\n"
            + "                    :mainTitle ?workTitle .\n"
            + "              optional {?work :publicationYear ?workYear .}"
            + "              }\n"
            + "}\n", remote().ontology());

    private static final String PLACE_OF_PUBLICATION_TO_INDEX_DOCUMENT_QUERY = format(""
            + "PREFIX  : <%1$s> \n"
            + "PREFIX  rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "select distinct ?placeuri ?place ?country\n"
            + "where {\n"
            + "    ?placeuri a :PlaceOfPublication ;\n"
            + "             :placename ?placename ;\n"
            + "             :country ?country .\n"
            + "}\n", remote().ontology());

    private final EntityService entityService;
    private final String elasticSearchBaseUrl;

    private ModelToIndexMapper placeOfPublicationModelToIndexMapper = modelToIndexMapperBuilder()
            .targetIndexType(PLACE_OF_PUBLICATION_TYPE)
            .selectQuery(PLACE_OF_PUBLICATION_TO_INDEX_DOCUMENT_QUERY)
            .mapFromResultVar("placename").toJsonPath("placename")
            .mapFromResultVar("country").toJsonPath("country")
            .build();

    public SearchServiceImpl(String elasticSearchBaseUrl, EntityService entityService) {
        this.elasticSearchBaseUrl = elasticSearchBaseUrl;
        this.entityService = entityService;
        getIndexUriBuilder();
    }

    @Override
    public final void indexWork(String workId) {
        doIndexWork(workId, false);
    }

    @Override
    public final void indexPerson(String personId) {
        doIndexPerson(personId, false);
    }

    @Override
    public final Response searchPersonWithJson(String json) {
        return searchWithJson(json, getPersonSearchUriBuilder());
    }

    @Override
    public final Response searchWorkWithJson(String json) {
        return searchWithJson(json, getWorkSearchUriBuilder());
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
            try (CloseableHttpResponse create = httpclient.execute(new HttpPut(uri))) {
                int statusCode = create.getStatusLine().getStatusCode();
                LOG.info("Create index request returned status " + statusCode);
                if (statusCode != HTTP_OK) {
                    throw new ServerErrorException("Failed to create elasticsearch index", HTTP_INTERNAL_ERROR);
                }
            }
            putIndexMapping(httpclient, "work");
            putIndexMapping(httpclient, "person");
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServerErrorException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private void putIndexMapping(CloseableHttpClient httpclient, String type) throws URISyntaxException, IOException {
        URI workIndexUri = getIndexUriBuilder().setPath("search/_mapping/" + type).build();
        HttpPut putWorkMappingRequest = new HttpPut(workIndexUri);
        putWorkMappingRequest.setEntity(new InputStreamEntity(getClass().getResourceAsStream("/" + type + "_mapping.json"), ContentType.APPLICATION_JSON));
        try (CloseableHttpResponse create = httpclient.execute(putWorkMappingRequest)) {
            int statusCode = create.getStatusLine().getStatusCode();
            LOG.info("Create mapping request for " + type + " returned status " + statusCode);
            if (statusCode != HTTP_OK) {
                throw new ServerErrorException("Failed to create elasticsearch mapping for " + type, HTTP_INTERNAL_ERROR);
            }
        }
    public void indexPlaceOfPublication(String id) { doIndexPlaceOfPublication(id); }

    @Override
    public Response searchPlaceOfPublicationWithJson(String json) {
        return searchWithJson(json, getPlaceOfPublicationUriBuilder());
    }

    private Response searchWithJson(String body, URIBuilder searchUriBuilder) {
        try {
            HttpPost httpPost = new HttpPost(searchUriBuilder.build());
            httpPost.setEntity(new StringEntity(body));
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
        return doSearch(query, getWorkSearchUriBuilder());
    }

    @Override
    public final Response searchPerson(String query) {
        return doSearch(query, getPersonSearchUriBuilder());
    }

    @Override
    public final Response searchPlaceOfPublication(String query) {
        return doSearch(query, getPlaceOfPublicationUriBuilder());
    }


    private void doIndexWork(String workId, boolean indexedPerson) {
        Model workModelWithLinkedResources = entityService.retrieveWorkWithLinkedResources(workId);
        indexModel(getworksModelToIndexMapper().modelToIndexDocument(workModelWithLinkedResources), "work");
        if (!indexedPerson) {
            Statement creatorProperty = workModelWithLinkedResources.getProperty(
                    createResource(remote().work() + workId),
                    CREATOR);
            if (creatorProperty != null) {
                String creatorUri = creatorProperty.getObject().asNode().getURI();
                doIndexPersonOnly(idFromEntityUri(creatorUri));
            }
        }
    }


    private void doIndexPerson(String personId, boolean indexedWork) {
        Model works = entityService.retrieveWorksByCreator(personId);
        if (!indexedWork) {
            ResIterator subjectIterator = works.listSubjects();
            while (subjectIterator.hasNext()) {
                String workUri = subjectIterator.next().toString();
                String workId = idFromEntityUri(workUri);
                doIndexWorkOnly(workId);
            }
        }
        Model personWithWorksModel = entityService.retrievePersonWithLinkedResources(personId).add(works);
        indexModel(getPersonModelToIndexMapper().modelToIndexDocument(personWithWorksModel), "person");
    }

    private void doIndexPlaceOfPublication(String id) {
        Model placeOfPublicationModel = entityService.retrieveById(EntityType.PLACE_OF_PUBLICATION, id);
        indexModel(placeOfPublicationModelToIndexMapper.modelToIndexDocument(placeOfPublicationModel), PLACE_OF_PUBLICATION_TYPE);
    }

    private void doIndexWorkOnly(String workId) {
        doIndexWork(workId, true);
    }

    private String idFromEntityUri(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    private void indexModel(Optional<Pair<String, String>> documentWithId, String indexType) {
        documentWithId.ifPresent(document -> indexDocument(indexType, document.getKey(), document.getValue()));
    }

    private void indexDocument(String type, String uri, String document) {
        try (CloseableHttpClient httpclient = createDefault()) {
            HttpPut httpPut = new HttpPut(getIndexUriBuilder()
                    .setPath(format("/search/%s/%s", type, encode(uri, UTF_8)))
                    .build());
            httpPut.setEntity(new StringEntity(document, Charset.forName(UTF_8)));
            httpPut.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.withCharset(UTF_8).toString());
            try (CloseableHttpResponse putResponse = httpclient.execute(httpPut)) {
                LOG.debug(putResponse.getStatusLine().toString());
            }
        } catch (Exception e) {
            LOG.error(format("Failed to index %s in elasticsearch", uri), e);
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

    private void doIndexPersonOnly(String personId) {
        doIndexPerson(personId, true);
    }

    private URIBuilder getIndexUriBuilder() {
        try {
            return new URIBuilder(this.elasticSearchBaseUrl);
        } catch (URISyntaxException e) {
            LOG.error("Failed to create uri builder for elasticsearch");
            throw new RuntimeException(e);
        }
    }

    private URIBuilder getWorkSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/work/_search");
    }

    private URIBuilder getPersonSearchUriBuilder() {
        return getIndexUriBuilder().setPath("/search/person/_search");
    }

    public URIBuilder getPlaceOfPublicationUriBuilder() { return getIndexUriBuilder().setPath("/search/placeOfPublication/_search"); }
}
