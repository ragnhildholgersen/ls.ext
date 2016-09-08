package no.deichman.services.entity.repository;

import no.deichman.services.entity.patch.Patch;
import no.deichman.services.entity.patch.PatchParser;
import no.deichman.services.entity.patch.PatchParserException;
import no.deichman.services.uridefaults.BaseURI;
import no.deichman.services.uridefaults.XURI;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Responsibility: TODO.
 */
public abstract class RDFRepositoryBase implements RDFRepository {

    private static final Resource PLACEHOLDER_RESOURCE = ResourceFactory.createResource("#");

    private final Logger log = LoggerFactory.getLogger(RDFRepositoryBase.class);
    private final SPARQLQueryBuilder sqb;
    private final UniqueURIGenerator uriGenerator;

    RDFRepositoryBase(SPARQLQueryBuilder sqb, UniqueURIGenerator uriGenerator) {
        this.sqb = sqb;
        this.uriGenerator = uriGenerator;
    }

    protected abstract QueryExecution getQueryExecution(Query query);

    protected abstract void executeUpdate(UpdateRequest updateRequest);

    @Override
    public final Model retrieveResourceByURI(XURI xuri) {
        log.debug("Attempting to retrieve resource <" + xuri.getUri() +">");
        try (QueryExecution qexec = getQueryExecution(sqb.getGetResourceByIdQuery(xuri.getUri()))) {
            disableCompression(qexec);
            return qexec.execDescribe();
        }
    }

    @Override
    public final Model retrieveWorkAndLinkedResourcesByURI(XURI xuri) {
        log.debug("Attempting to retrieve: <" + xuri.getUri() + ">");
        try (QueryExecution qexec = getQueryExecution(sqb.describeWorkAndLinkedResources(xuri))) {
            disableCompression(qexec);
            return qexec.execDescribe();
        }
    }

    @Override
    public final Model retrievePersonAndLinkedResourcesByURI(String uri) {
        log.debug("Attempting to retrieve person: " + uri);
        try (QueryExecution qexec = getQueryExecution(sqb.describePersonAndLinkedResources(uri))) {
            disableCompression(qexec);
            return qexec.execDescribe();
        }
    }

    @Override
    public final Model retrieveCorporationAndLinkedResourcesByURI(String uri) {
        log.debug("Attempting to retrieve corporation: " + uri);
        try (QueryExecution qexec = getQueryExecution(sqb.describeCorporationAndLinkedResources(uri))) {
            disableCompression(qexec);
            return qexec.execDescribe();
        }
    }

    @Override
    public final void updateWork(String work) {
        InputStream stream = new ByteArrayInputStream(work.getBytes(StandardCharsets.UTF_8));
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, stream, Lang.JSONLD);

        UpdateRequest updateRequest = UpdateFactory.create(sqb.getUpdateWorkQueryString(model));
        executeUpdate(updateRequest);
    }

    @Override
    public final void updateResource(String query) {
        UpdateRequest updateRequest = UpdateFactory.create(query);
        executeUpdate(updateRequest);
    }

    @Override
    public final XURI retrieveWorkByRecordId(String recordId) throws Exception {
        try (QueryExecution qexec = getQueryExecution(sqb.getWorkByRecordId(recordId))) {
            disableCompression(qexec);
            XURI returnValue = null;
            ResultSet resultSet = qexec.execSelect();

            if (resultSet.hasNext()) {
                returnValue = new XURI(resultSet.next().getResource("work").toString());
            } else {
                throw new NotFoundException();
            }
            return returnValue;
        }

    }

    @Override
    public final boolean askIfResourceExists(XURI xuri) {
        try (QueryExecution qexec = getQueryExecution(sqb.checkIfResourceExists(xuri))) {
            disableCompression(qexec);
            return qexec.execAsk();
        }
    }

    @Override
    public final String createWork(Model inputModel) throws Exception {
        String type = "Work";
        return createResource(inputModel, type);
    }

    private String createResource(Model inputModel, String type) throws Exception {
        inputModel.add(tempTypeStatement(type));
        String uri = uriGenerator.getNewURI(type, this::askIfResourceExists);

        UpdateAction.parseExecute(sqb.getReplaceSubjectQueryString(uri), inputModel);

        UpdateRequest updateRequest = UpdateFactory.create(sqb.getCreateQueryString(inputModel));
        executeUpdate(updateRequest);
        return uri;
    }

    @Override
    public final String createPerson(Model inputModel) throws Exception {
        return createResource(inputModel, "Person");
    }

    @Override
    public final String createPlace(Model inputModel) throws Exception {
        return createResource(inputModel, "Place");
    }

    @Override
    public final String createCorporation(Model inputModel) throws Exception {
        return createResource(inputModel, "Corporation");
    }

    @Override
    public final String createSerial(Model inputModel) throws Exception {
        return createResource(inputModel, "Serial");
    }

    @Override
    public final String createSubject(Model inputModel) throws Exception {
        String type = "Subject";
        inputModel.add(tempTypeStatement(type));
        String uri = uriGenerator.getNewURI(type, this::askIfResourceExists);
        UpdateAction.parseExecute(sqb.getReplaceSubjectQueryString(uri), inputModel);

        UpdateRequest updateRequest = UpdateFactory.create(sqb.getCreateQueryString(inputModel));
        executeUpdate(updateRequest);
        return uri;
    }

    @Override
    public final String createGenre(Model inputModel) throws Exception {
        return createResource(inputModel, "Genre");
    }

    @Override
    public final String createMusicalInstrument(Model inputModel) throws Exception {
        return createResource(inputModel, "Instrument");
    }

    @Override
    public final String createMusicalCompositionType(Model inputModel) throws Exception {
        return createResource(inputModel, "CompositionType");
    }

    @Override
    public final String createEvent(Model inputModel) throws Exception {
        return createResource(inputModel, "Event");
    }

    @Override
    public final void createResource(Model inputModel) throws Exception {
        UpdateRequest updateRequest = UpdateFactory.create(sqb.getCreateQueryString(inputModel));
        executeUpdate(updateRequest);
    }

    @Override
    public final String createPublication(Model inputModel, String recordID) throws Exception {
        String type = "Publication";

        inputModel.add(tempTypeStatement(type));
        String uri = uriGenerator.getNewURI(type, this::askIfResourceExists);

        inputModel.add(tempRecordIdStatement(recordID));

        UpdateAction.parseExecute(sqb.getReplaceSubjectQueryString(uri), inputModel);
        UpdateRequest updateRequest = UpdateFactory.create(sqb.getCreateQueryString(inputModel));
        executeUpdate(updateRequest);
        return uri;
    }

    private Statement tempRecordIdStatement(String recordID) {
        return ResourceFactory.createStatement(
                PLACEHOLDER_RESOURCE,
                ResourceFactory.createProperty(BaseURI.ontology() + "recordID"),
                ResourceFactory.createTypedLiteral(recordID, XSDDatatype.XSDstring));
    }

    private Statement tempTypeStatement(String clazz) {
        return ResourceFactory.createStatement(
                PLACEHOLDER_RESOURCE,
                RDF.type,
                ResourceFactory.createResource(BaseURI.ontology() + clazz));
    }

    @Override
    public final boolean askIfStatementExists(Statement statement) {
        try (QueryExecution qexec = getQueryExecution(sqb.checkIfStatementExists(statement))) {
            disableCompression(qexec);
            return qexec.execAsk();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public final void delete(Model inputModel) {
        UpdateRequest updateRequest = null;
        try {
            updateRequest = UpdateFactory.create(sqb.patch(PatchParser.createDeleteModelAsPatches(inputModel)));
        } catch (PatchParserException e) {
            throw new RuntimeException(e);
        }
        executeUpdate(updateRequest);
    }

    @Override
    public final void patch(List<Patch> patches) {
        UpdateRequest updateRequest = UpdateFactory.create(sqb.patch(patches));
        executeUpdate(updateRequest);
    }

    @Override
    public final Optional<String> getResourceURIByBibliofilId(String id, String type) {
        try (QueryExecution qexec = getQueryExecution(sqb.getImportedResourceById(id, type))) {
            disableCompression(qexec);
            ResultSet resultSet = qexec.execSelect();
            boolean uri = resultSet.hasNext();
            if (uri) {
                return Optional.of(resultSet.next().getResource("uri").toString());
            }
        }
        return Optional.empty();
    }


    @Override
    public final Model retrieveWorksByCreator(XURI xuri) {
        log.debug("Attempting to retrieve: works created by " + xuri.getUri());
        try (QueryExecution qexec = getQueryExecution(sqb.describeWorksByCreator(xuri))) {
            disableCompression(qexec);
            return qexec.execDescribe();
        }
    }

    @Override
    public final Model retrievePublicationsByWork(XURI xuri) {
        log.debug("Attempting to retrieve: " + xuri.getUri());
        try (QueryExecution qexec = getQueryExecution(sqb.describeLinkedPublications(xuri))) {
            disableCompression(qexec);
            return qexec.execDescribe();
        }
    }

    @Override
    public final void findAllUrisOfType(String type, Consumer<String> consumer) {
        log.debug("Attempting to retrieve all " + type + " uris: ");
        try (QueryExecution qexec = getQueryExecution(sqb.selectAllUrisOfType(type))) {
            disableCompression(qexec);
            qexec.execSelect().forEachRemaining(querySolution -> {
                consumer.accept(querySolution.get("uri").asResource().getURI());
            });
        }
    }

    @Override
    public final List<String> getWorkURIsByAgent(XURI agent) {
        List<String> res = new ArrayList<>();
        try (QueryExecution qexec = getQueryExecution(sqb.selectWorksByAgent(agent))) {
            disableCompression(qexec);
            qexec.execSelect().forEachRemaining(querySolution -> {
                res.add(querySolution.get("work").asResource().getURI());
            });
        }
        return res;
    }


    private void disableCompression(QueryExecution qexec) {
        if (qexec instanceof QueryEngineHTTP) {
            ((QueryEngineHTTP) qexec).setAllowGZip(false);
            ((QueryEngineHTTP) qexec).setAllowDeflate(false);
        }
    }
}
