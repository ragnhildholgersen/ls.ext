package no.deichman.services.entity;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import no.deichman.services.entity.kohaadapter.KohaAdapter;
import no.deichman.services.entity.patch.PatchParser;
import no.deichman.services.entity.patch.PatchParserException;
import no.deichman.services.entity.repository.RDFRepository;
import no.deichman.services.entity.repository.SPARQLQueryBuilder;
import no.deichman.services.uridefaults.BaseURI;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsibility: TODO.
 */
public final class EntityServiceImpl implements EntityService {

    private final RDFRepository repository;
    private final KohaAdapter kohaAdapter;
    private final BaseURI baseURI;
    private final Property recordID;
    private static final String LANGUAGE_TTL_FILE = "language.ttl";
    private static final String FORMAT_TTL_FILE = "format.ttl";

    private Model getLinkedLexvoResource(Model input) {

        NodeIterator objects = input.listObjects();
        if (objects.hasNext()) {
            Set<RDFNode> objectResources = objects.toSet();
            objectResources.stream()
                    .filter(node -> node.toString()
                            .contains("http://lexvo.org/id/iso639-3/")).collect(Collectors.toList())
                    .forEach(lv -> {
                        input.add(extractNamedResourceFromModel(lv.toString(), EntityServiceImpl.class.getClassLoader().getResourceAsStream(LANGUAGE_TTL_FILE), Lang.TURTLE));
                    });
        }

        return input;
    }

    private Model getLinkedFormatResource(Model input) {

        NodeIterator objects = input.listObjects();
        if (objects.hasNext()) {
            Set<RDFNode> objectResources = objects.toSet();
            objectResources.stream()
                    .filter(node -> node.toString()
                            .contains("http://data.deichman.no/format#")).collect(Collectors.toList())
                    .forEach(result -> {
                        input.add(extractNamedResourceFromModel(result.toString(), EntityServiceImpl.class.getClassLoader().getResourceAsStream(FORMAT_TTL_FILE), Lang.TURTLE));
                    });
        }

        return input;
    }
    private Model extractNamedResourceFromModel(String resource, InputStream input, Lang lang) {
        Model tempModel = ModelFactory.createDefaultModel();
        RDFDataMgr.read(tempModel, input, lang);
        QueryExecution qexec = QueryExecutionFactory.create(
                QueryFactory.create("DESCRIBE <" + resource + ">"),
                tempModel);
        return qexec.execDescribe();
    }

    public EntityServiceImpl(BaseURI baseURI, RDFRepository repository, KohaAdapter kohaAdapter){
        this.baseURI = baseURI;
        recordID = ResourceFactory.createProperty(this.baseURI.ontology() + "recordID");
        this.repository = repository;
        this.kohaAdapter = kohaAdapter;
    }

    @Override
    public Model retrieveById(EntityType type, String id) {
        Model m = ModelFactory.createDefaultModel();
        switch (type) {
            case PUBLICATION:
                m.add(repository.retrievePublicationById(id));
                break;
            case WORK:
                m.add(repository.retrieveWorkById(id));
                m = getLinkedLexvoResource(m);
                m = getLinkedFormatResource(m);
                break;
            default:
                throw new IllegalArgumentException("Unknown entity type:" + type);
        }
        return m;
    }


    @Override
    public void updateWork(String work) {
        repository.updateWork(work);
    }

    @Override
    public Model retrieveWorkItemsById(String id) {
        Model allItemsModel = ModelFactory.createDefaultModel();

        Model workModel = repository.retrieveWorkById(id);
        QueryExecution queryExecution = QueryExecutionFactory.create(QueryFactory.create(
                "PREFIX  deichman: <" + baseURI.ontology() + "> "
                        + "SELECT  * "
                        + "WHERE { ?publicationUri deichman:recordID ?biblioId }"), workModel);
        ResultSet publicationSet = queryExecution.execSelect();

        while (publicationSet.hasNext()) {
            QuerySolution solution =  publicationSet.next();
            RDFNode publicationUri = solution.get("publicationUri");
            String biblioId = solution.get("biblioId").asLiteral().getString();

            Model itemsForBiblio = kohaAdapter.getBiblio(biblioId);

            SPARQLQueryBuilder sqb = new SPARQLQueryBuilder(baseURI);
            Query query = sqb.getItemsFromModelQuery(publicationUri.toString());
            QueryExecution qexec = QueryExecutionFactory.create(query, itemsForBiblio);
            Model itemsModelWithBlankNodes = qexec.execConstruct();

            allItemsModel.add(itemsModelWithBlankNodes);
        }
        return allItemsModel;
    }

    @Override
    public String create(EntityType type, String jsonLd) {
        String uri = null;
        switch (type) {
            case PUBLICATION:
                String recordId = kohaAdapter.getNewBiblio();
                uri = repository.createPublication(jsonLd, recordId);
                break;
            case WORK:
                uri = repository.createWork(jsonLd);
                break;
            default:
                throw new IllegalArgumentException("Unknown entity type:" + type);
        }
        return uri;
    }

    @Override
    public void delete(Model model) {
        repository.delete(model);
    }

    @Override
    public Model patch(EntityType type, String id, String ldPatchJson) throws PatchParserException {
        repository.patch(PatchParser.parse(ldPatchJson));
        return retrieveById(type, id);
    }

    @Override
    public boolean resourceExists(String resourceUri) {
        return repository.askIfResourceExists(resourceUri);
    }
}
