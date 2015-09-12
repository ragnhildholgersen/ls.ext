package no.deichman.services;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.body.RequestBodyEntity;
import no.deichman.services.entity.kohaadapter.KohaSvcMock;
import no.deichman.services.rdf.RDFModelUtil;
import no.deichman.services.restutils.MimeType;
import no.deichman.services.testutil.PortSelector;
import org.apache.jena.riot.Lang;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response.Status;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AppTest {

    private static final boolean USE_IN_MEMORY_REPO = true;
    private static final String LOCALHOST = "http://127.0.0.1";
    private static final String FIRST_BIBLIO_ID = "111111";
    private static final String SECOND_BIBLIO_ID = "222222";
    private static final String ANY_URI = "http://www.w3.org/2001/XMLSchema#anyURI";
    private String baseUri;
    private App app;

    private KohaSvcMock kohaSvcMock;

    @Before
    public void setUp() throws Exception {
        int appPort = PortSelector.randomFree();
        baseUri = LOCALHOST + ":" + appPort + "/";
        System.setProperty("DATA_BASEURI", baseUri);
        kohaSvcMock = new KohaSvcMock();
        String svcEndpoint = LOCALHOST + ":" + kohaSvcMock.getPort();
        app = new App(appPort, svcEndpoint, USE_IN_MEMORY_REPO);
        app.startAsync();
    }

    @After
    public void tearDown() throws Exception {
        app.stop();
    }

    @Test
    public void happy_day_scenario() throws Exception {

        kohaSvcMock.addLoginExpectation();
        kohaSvcMock.addPostNewBiblioExpectation(FIRST_BIBLIO_ID);

        final HttpResponse<JsonNode> createPublicationResponse = buildCreateRequest(baseUri + "publication").asJson();

        assertResponse(Status.CREATED, createPublicationResponse);
        final String publicationUri = getLocation(createPublicationResponse);
        assertIsUri(publicationUri);
        assertThat(publicationUri, startsWith(baseUri));

        final HttpResponse<JsonNode> createWorkResponse = buildCreateRequest(baseUri + "work").asJson();

        assertResponse(Status.CREATED, createWorkResponse);
        final String workUri = getLocation(createWorkResponse);
        assertIsUri(workUri);
        assertThat(workUri, startsWith(baseUri));

        final JsonArray workIntoPublicationPatch = buildLDPatch(buildPatchStatement("add", publicationUri, baseUri + "ontology#publicationOf", workUri, ANY_URI));

        final HttpResponse<String> patchWorkIntoPublicationResponse = buildPatchRequest(publicationUri, workIntoPublicationPatch).asString();
        assertResponse(Status.OK, patchWorkIntoPublicationResponse);

        kohaSvcMock.addPostNewBiblioExpectation(SECOND_BIBLIO_ID);

        final HttpResponse<JsonNode> createSecondPublicationResponse = buildCreateRequest(baseUri + "publication").asJson();
        assertResponse(Status.CREATED, createSecondPublicationResponse);
        final String secondPublicationUri = getLocation(createSecondPublicationResponse);

        final JsonArray workIntoSecondPublicationPatch = buildLDPatch(buildPatchStatement("add", secondPublicationUri, baseUri + "ontology#publicationOf", workUri, ANY_URI));
        final HttpResponse<String> patchWorkIntoSecondPublicationResponse = buildPatchRequest(secondPublicationUri, workIntoSecondPublicationPatch).asString();
        assertResponse(Status.OK, patchWorkIntoSecondPublicationResponse);

        final HttpResponse<JsonNode> getWorkWithTwoPublications = buildGetRequest(workUri).asJson();

        assertResponse(Status.OK, getWorkWithTwoPublications);
        final JsonNode workWith2Publications = getWorkWithTwoPublications.getBody();
        assertThat(workWith2Publications, notNullValue());

        final Model workWith2PublicationsModel = RDFModelUtil.modelFrom(workWith2Publications.toString(), Lang.JSONLD);

        final QueryExecution workWith2PublicationsCount = QueryExecutionFactory.create(
                QueryFactory.create(
                        "PREFIX deichman: <" + baseUri + "ontology#>"
                                + "SELECT (COUNT (?publication) AS ?noOfPublications) { "
                                + "<" + workUri + "> a deichman:Work ."
                                + "?publication a deichman:Publication ."
                                + "?publication deichman:publicationOf <" + workUri + "> ."
                                + "}"), workWith2PublicationsModel);
        assertThat("model does not have a work with two publications",
                workWith2PublicationsCount.execSelect().next().getLiteral("noOfPublications").getInt(),
                equalTo(2));

        // Two publications with a total of three items
        kohaSvcMock.addGetBiblioExpectation(FIRST_BIBLIO_ID, 2);
        kohaSvcMock.addGetBiblioExpectation(SECOND_BIBLIO_ID, 1);

        final HttpResponse<JsonNode> getWorkWith2Plus1ItemsResponse = buildGetItemsRequest(workUri).asJson();
        final JsonNode work1Plus2Items = getWorkWith2Plus1ItemsResponse.getBody();
        final Model work1Plus2ItemsModel = RDFModelUtil.modelFrom(work1Plus2Items.toString(), Lang.JSONLD);

        final QueryExecution workWith1Plus2ItemsCount = QueryExecutionFactory.create(
                QueryFactory.create(
                        "PREFIX deichman: <" + baseUri + "ontology#>"
                                + "SELECT (COUNT (?item) AS ?noOfItems) { "
                                + "?item a deichman:Item ."
                                + "}"), work1Plus2ItemsModel);
        assertThat("model does not three publications",
                workWith1Plus2ItemsCount.execSelect().next().getLiteral("noOfItems").getInt(),
                equalTo(2 + 1));
    }

    @Test
    public void get_authorized_values_for_language() throws Exception {
        HttpRequest languageRequest = Unirest
                .get(baseUri + "authorized_values/language")
                .header("Accept", "application/ld+json");
        HttpResponse<?> languageResponse = languageRequest.asString();
        assertResponse(Status.OK, languageResponse);

        Model model = RDFModelUtil.modelFrom(languageResponse.getBody().toString(), Lang.JSONLD);
        boolean hasEnglish = model.contains(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://lexvo.org/id/iso639-3/eng"),
                RDFS.label,
                ResourceFactory.createLangLiteral("Engelsk", "no")
        ));
        assertTrue("model doesn't have English", hasEnglish);
    }

    @Test
    public void get_authorized_values_for_format() throws Exception {
        HttpRequest formatRequest = Unirest
                .get(baseUri + "authorized_values/format")
                .header("Accept", "application/ld+json");
        HttpResponse<?> formatResponse = formatRequest.asString();
        assertResponse(Status.OK, formatResponse);

        Model model = RDFModelUtil.modelFrom(formatResponse.getBody().toString(), Lang.JSONLD);
        boolean hasBook = model.contains(ResourceFactory.createStatement(
                ResourceFactory.createResource("http://data.deichman.no/format#Book"),
                RDFS.label,
                ResourceFactory.createLangLiteral("Bok", "no")
        ));
        assertTrue("model doesn't have Book", hasBook);
    }

    @Test
    public void get_ontology() throws Exception {
        HttpRequest request = Unirest
                .get(baseUri + "ontology")
                .header("Accept", MimeType.LD_JSON);
        HttpResponse<?> response = request.asString();
        assertResponse(Status.OK, response);

        Model ontology = RDFModelUtil.modelFrom(response.getBody().toString(), Lang.JSONLD);
        Statement workStatement = ResourceFactory.createStatement(
                ResourceFactory.createResource(baseUri + "ontology#Work"),
                RDFS.label,
                ResourceFactory.createLangLiteral("Verk", "no")
        );
        assertTrue("ontology doesn't have Work", ontology.contains(workStatement));
    }

    private GetRequest buildGetItemsRequest(String workUri) {
        return Unirest
                .get(workUri + "/items")
                .header("Accept", "application/ld+json");
    }

    private GetRequest buildGetRequest(String workUri) {
        return Unirest
                .get(workUri)
                .header("Accept", "application/ld+json");
    }

    private static JsonObjectBuilder buildPatchStatement(String op, String s, String p, String o, String type) {
        return Json.createObjectBuilder()
                .add("op", op).add("s", s).add("p", p).add("o", Json.createObjectBuilder().add("value", o).add("type", type));
    }

    private static JsonArray buildLDPatch(JsonObjectBuilder... patchStatements) {
        JsonArrayBuilder patchBuilder = Json.createArrayBuilder();
        for (JsonObjectBuilder patchStatement : patchStatements) {
            patchBuilder.add(patchStatement);
        }
        return patchBuilder.build();
    }

    private static String getLocation(HttpResponse<?> response) {
        return response.getHeaders().getFirst("Location");
    }

    private static RequestBodyEntity buildPatchRequest(String uri, JsonArray patch) {
        return Unirest
                    .patch(uri)
                    .header("Accept", "application/ld+json")
                    .header("Content-Type", "application/ldpatch+json")
                    .body(new JsonNode(patch.toString()));
    }

    private static void assertIsUri(String uri) {
        assertTrue("Not a URI: " + uri, uri.matches("(?:http|https)(?::\\/{2}[\\w]+)(?:[\\/|\\.]?)(?:[^\\s]*)"));
    }

    private static void assertResponse(Status status, HttpResponse<?> response) {
        assertThat("Unexpected response: " + response.getBody(),
                Status.fromStatusCode(response.getStatus()),
                equalTo(status)
        );
    }

    private static RequestBodyEntity buildCreateRequest(String uri) {
        return Unirest
                .post(uri)
                .header("Accept", "application/ld+json")
                .header("Content-Type", "application/ld+json")
                .body("{}");
    }

}
