package no.deichman.services.service;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import no.deichman.services.error.PatchParserException;
import no.deichman.services.kohaadapter.KohaAdapter;
import no.deichman.services.kohaadapter.Marc2Rdf;
import no.deichman.services.repository.Repository;
import no.deichman.services.repository.RepositoryInMemory;
import no.deichman.services.uridefaults.BaseURI;
import no.deichman.services.uridefaults.BaseURIMock;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceImplTest {

    private ServiceImpl service;
    private Repository repository;
    private String ontologyURI;
    private String workURI;
    private String publicationURI;

    private String getTestJSON(String id, String type) {
        String resourceClass = null;
        String resourceURI = null;

        if (type.toLowerCase().equals("work")) {
            resourceClass = "Work";
            resourceURI = workURI;
        } else if (type.toLowerCase().equals("publication")) {
            resourceClass = "Publication";
            resourceURI = publicationURI;
        }

        return "{\"@context\": "
                + "{\"dcterms\": \"http://purl.org/dc/terms/\","
                + "\"deichman\": \"" + ontologyURI + "\"},"
                + "\"@graph\": "
                + "{\"@id\": \"" + resourceURI + "" + id + "\","
                + "\"@type\": \"deichman:" + resourceClass + "\","
                + "\"dcterms:identifier\":\"" + id + "\"}}";
    }

    private String getTestPatch(String operation, String id) {
        return "{"
        + "\"op\": \"add\","
        + "\"s\": \"" + id + "\","
        + "\"p\": \"" + ontologyURI + "color\","
        + "\"o\": {"
        + "\"value\": \"red\""
        + "}"
        + "}";
    }

    @Before
    public void setup(){
        BaseURI baseURI = new BaseURIMock();
        repository = new RepositoryInMemory();
        service = new ServiceImpl(baseURI, repository, null);
        ontologyURI = baseURI.getOntologyURI();
        workURI = baseURI.getWorkURI();
        publicationURI = baseURI.getPublicationURI();
    }

    @Test
    public void should_have_default_constructor() {
        assertNotNull(new ServiceImpl());
    }

    @Test
    public void test_retrieve_work_by_id(){
        String testId = "work_SHOULD_EXIST";
        String workData = getTestJSON(testId,"work");
        String workId = service.createWork(workData);
        Model comparison = ModelFactory.createDefaultModel();
        InputStream in = new ByteArrayInputStream(
                workData.replace(workURI + testId, workId)
                .getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(comparison, in, Lang.JSONLD);
        Model test = service.retrieveWorkById(workId.replace(workURI, ""));
        assertTrue(test.isIsomorphicWith(comparison));
    }

    @Test
    public void test_retrieve_publication_by_id() throws Exception{
        String testId = "publication_SHOULD_EXIST";
        String publicationData = getTestJSON(testId,"publication");
        String publicationId = service.createPublication(publicationData);
        Model test = service.retrievePublicationById(publicationId.replace(publicationURI, ""));
        Statement testStatement = ResourceFactory.createStatement(
                ResourceFactory.createResource(publicationId),
                ResourceFactory.createProperty(RDF.type.getURI()),
                ResourceFactory.createResource(ontologyURI + "Publication"));
        assertTrue(test.contains(testStatement));
    }

    @Test
    public void test_repository_can_be_set_got(){
        String testId = "work_TEST_REPOSITORY";
        String workData = getTestJSON(testId,"work");
        String workId = service.createWork(workData);
        Statement s = ResourceFactory.createStatement(
                ResourceFactory.createResource(workId),
                ResourceFactory.createProperty(DCTerms.identifier.getURI()),
                ResourceFactory.createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
    }

    private Model modelForBiblio() { // TODO return much simpler model
        Model model = ModelFactory.createDefaultModel();
        Model m = ModelFactory.createDefaultModel();
        InputStream in = getClass().getClassLoader().getResourceAsStream("marc.xml");
        MarcReader reader = new MarcXmlReader(in);
        Marc2Rdf marcRdf = new Marc2Rdf(new BaseURIMock());
        while (reader.hasNext()) {
            Record record = reader.next();
            m.add(marcRdf.mapItemsToModel(record.getVariableFields("952")));
        }
        model.add(m);
        return model;
    }

    @Test
    public void test_retrieve_work_items_by_id(){
        KohaAdapter mockKohaAdapter = mock(KohaAdapter.class);
        when(mockKohaAdapter.getBiblio("626460")).thenReturn(modelForBiblio());

        Service myService = new ServiceImpl(new BaseURIMock(), repository, mockKohaAdapter);
        Model m = myService.retrieveWorkItemsById("work_TEST_KOHA_ITEMS_LINK");
        Property p = ResourceFactory.createProperty(ontologyURI + "hasEdition");
        NodeIterator ni = m.listObjectsOfProperty(p);

        int i = 0;
        while (ni.hasNext()) {
            i++;
            ni.next();
        }
        final int expectedNoOfItems = 34;
        assertEquals(expectedNoOfItems,i);
    }

    @Test
    public void test_create_work(){
        String testId = "SERVICE_WORK_SHOULD_EXIST";
        String work = getTestJSON(testId,"work");
        Statement s = ResourceFactory.createStatement(
                ResourceFactory.createResource(service.createWork(work)),
                ResourceFactory.createProperty(DCTerms.identifier.getURI()),
                ResourceFactory.createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
    }

    @Test
    public void test_create_publication() throws Exception{
        String testId = "publication_SERVICE_CREATE_PUBLICATION";
        String publication = getTestJSON(testId, "publication");
        String publicationId = service.createPublication(publication);
        Statement s = ResourceFactory.createStatement(
                ResourceFactory.createResource(publicationId),
                ResourceFactory.createProperty(DCTerms.identifier.getURI()),
                ResourceFactory.createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
    }

    @Test
    public void test_delete_work(){
        String testId = "work_SHOULD_BE_DELETED";
        String work = getTestJSON(testId,"work");
        String workId = service.createWork(work);
        Statement s = ResourceFactory.createStatement(
                ResourceFactory.createResource(workId),
                ResourceFactory.createProperty(DCTerms.identifier.getURI()),
                ResourceFactory.createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
        Model test = ModelFactory.createDefaultModel();
        InputStream in = new ByteArrayInputStream(
                work.replace(workURI + testId, workId)
                .getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(test,in, Lang.JSONLD);
        service.deleteWork(test);
        assertFalse(repository.askIfStatementExists(s));
    }

    @Test
    public void test_delete_publication() throws Exception{
        String testId = "publication_SHOULD_BE_DELETED";
        String publication = getTestJSON(testId, "publication");
        String publicationId = service.createPublication(publication);
        Statement s = ResourceFactory.createStatement(
                ResourceFactory.createResource(publicationId),
                ResourceFactory.createProperty(DCTerms.identifier.getURI()),
                ResourceFactory.createPlainLiteral(testId));
        assertTrue(repository.askIfStatementExists(s));
        Model test = ModelFactory.createDefaultModel();
        InputStream in = new ByteArrayInputStream(
                publication.replace(publicationURI + testId, publicationId)
                .getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(test,in, Lang.JSONLD);
        service.deleteWork(test);
        assertFalse(repository.askIfStatementExists(s));
    }

    @Test
    public void test_patch_work_add() throws Exception{
        String testId = "work_SHOULD_BE_PATCHABLE";
        String workData = getTestJSON(testId, "work");
        String workId = service.createWork(workData);

        Model oldModel = ModelFactory.createDefaultModel();

        String comparisonRDF = workData.replace(workURI + testId, workId);
        InputStream oldIn = new ByteArrayInputStream(comparisonRDF.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(oldModel, oldIn, Lang.JSONLD);
        String nonUriWorkId = workId.replace(workURI, "");
        Model data = service.retrieveWorkById(nonUriWorkId);
        assertTrue(oldModel.isIsomorphicWith(data));
        String patchData = getTestPatch("add", workId);
        Model patchedModel = service.patchWork(nonUriWorkId, patchData);
        assertTrue(patchedModel.contains(
                ResourceFactory.createResource(workId),
                ResourceFactory.createProperty(ontologyURI + "color"),
                "red"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFDataMgr.write(baos,patchedModel.difference(oldModel),Lang.NT);
        assertEquals(baos.toString().trim(),
                "<"+ workId + "> <" + ontologyURI + "color> \"red\" .");
    }

    @Test
    public void test_patch_publication_add() throws Exception{
        String testId = "publication_SHOULD_BE_PATCHABLE";
        String publicationData = getTestJSON(testId,"publication");
        String publicationId = service.createPublication(publicationData);
        String nonUriPublicationId = publicationId.replace(publicationURI, "");
        String patchData = getTestPatch("add", publicationId);
        Model patchedModel = service.patchPublication(nonUriPublicationId, patchData);
        assertTrue(patchedModel.contains(
                ResourceFactory.createResource(publicationId),
                ResourceFactory.createProperty(ontologyURI + "color"),
                "red"));
    }

    @Test(expected=PatchParserException.class)
    public void test_bad_patch_fails() throws Exception{
        String workData = getTestJSON("SHOULD_FAIL","work");
        String workId = service.createWork(workData);
        String badPatchData = "{\"po\":\"cas\",\"s\":\"http://example.com/a\"}";
        service.patchWork(workId.replace(workURI, ""),badPatchData);
    }
}