package no.deichman.services.repository;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import no.deichman.services.patch.Patch;
import no.deichman.services.uridefaults.BaseURI;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * Responsibility: TODO.
 */
public abstract class FusekiRepository implements Repository {

    public static final Resource PLACEHOLDER_RESOURCE = ResourceFactory.createResource("#");

    private final BaseURI baseURI;
    private final SPARQLQueryBuilder sqb;
    private final UniqueURIGenerator uriGenerator;

    FusekiRepository(BaseURI baseURI, SPARQLQueryBuilder sqb, UniqueURIGenerator uriGenerator) {
        this.baseURI = baseURI;
        this.sqb = sqb;
        this.uriGenerator = uriGenerator;
    }

    protected abstract QueryExecution getQueryExecution(Query query);

    protected abstract void executeUpdate(UpdateRequest updateRequest);

    @Override
    public final Model retrieveWorkById(String id) {
        String uri = baseURI.getWorkURI() + id;
        System.out.println("Attempting to retrieve: " + uri);
        try (QueryExecution qexec = getQueryExecution(sqb.describeWorkAndLinkedPublication(uri))) {
            return qexec.execDescribe();
        }
    }

    @Override
    public final Model retrievePublicationById(final String id) {
        String uri = baseURI.getPublicationURI() + id;
        System.out.println("Attempting to retrieve: " + uri);
        try (QueryExecution qexec = getQueryExecution(sqb.getGetResourceByIdQuery(uri))) {
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
    public final boolean askIfResourceExists(String uri) {
        try (QueryExecution qexec = getQueryExecution(sqb.checkIfResourceExists(uri))){
            return qexec.execAsk();
        }
    }

    @Override
    public final String createWork(String work) {
        InputStream stream = new ByteArrayInputStream(work.getBytes(StandardCharsets.UTF_8));
        String uri = uriGenerator.getNewURI("work", this::askIfResourceExists);
        Model tempModel = ModelFactory.createDefaultModel();
        Statement workResource = ResourceFactory.createStatement(
                PLACEHOLDER_RESOURCE,
                RDF.type,
                ResourceFactory.createResource(baseURI.getOntologyURI() + "Work"));
        tempModel.add(workResource);
        RDFDataMgr.read(tempModel, stream, Lang.JSONLD);

        UpdateAction.parseExecute(sqb.getReplaceSubjectQueryString(uri), tempModel);
        UpdateRequest updateRequest = UpdateFactory.create(sqb.getCreateQueryString(tempModel));
        executeUpdate(updateRequest);
        return uri;
    }

    @Override
    public final String createPublication(String publication, String recordID) {
        InputStream stream = new ByteArrayInputStream(publication.getBytes(StandardCharsets.UTF_8));
        String uri = uriGenerator.getNewURI("publication", this::askIfResourceExists);
        Model tempModel = ModelFactory.createDefaultModel();
        Statement publicationResource = ResourceFactory.createStatement(
                PLACEHOLDER_RESOURCE,
                RDF.type,
                ResourceFactory.createResource(baseURI.getOntologyURI() + "Publication"));
        Statement recordLink = ResourceFactory.createStatement(
                PLACEHOLDER_RESOURCE,
                ResourceFactory.createProperty(baseURI.getOntologyURI() + "recordID"),
                ResourceFactory.createTypedLiteral(recordID, XSDDatatype.XSDstring));
        tempModel.add(publicationResource);
        tempModel.add(recordLink);
        RDFDataMgr.read(tempModel, stream, Lang.JSONLD);

        UpdateAction.parseExecute(sqb.getReplaceSubjectQueryString(uri), tempModel);
        UpdateRequest updateRequest = UpdateFactory.create(sqb.getCreateQueryString(tempModel));
        executeUpdate(updateRequest);
        return uri;
    }

    @Override
    public final boolean askIfStatementExists(Statement statement) {
        try (QueryExecution qexec = getQueryExecution(sqb.checkIfStatementExists(statement))) {
            return qexec.execAsk();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public final void delete(Model inputModel) {
        UpdateRequest updateRequest = UpdateFactory.create(sqb.updateDelete(inputModel));
        executeUpdate(updateRequest);
    }

    @Override
    public final void patch(List<Patch> patches) {
        UpdateRequest updateRequest = UpdateFactory.create(sqb.patch(patches));
        executeUpdate(updateRequest);
    }
}
