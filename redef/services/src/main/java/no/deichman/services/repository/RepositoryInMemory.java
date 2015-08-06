package no.deichman.services.repository;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateRequest;
import no.deichman.services.uridefaults.BaseURI;
import no.deichman.services.uridefaults.BaseURIMock;

/**
 * Responsibility: Implement an in-memory RDF-repository.
 */
public final class RepositoryInMemory extends FusekiRepository {
    private final Dataset model;

    public RepositoryInMemory() {
        this(new BaseURIMock(), new SPARQLQueryBuilder(new BaseURIMock()), new UniqueURIGenerator(new BaseURIMock()));
    }

    public RepositoryInMemory(BaseURI baseURI, SPARQLQueryBuilder sqb, UniqueURIGenerator uriGenerator) {
        super(baseURI, sqb, uriGenerator);
        model = DatasetFactory.createMem();
        System.out.println("In-memory repository started.");
    }

    public void addData(Model newData){
        model.getDefaultModel().add(newData);
    }

    public Model getModel() {
        return model.getDefaultModel();
    }

    public Dataset getDataset() {
        return model;
    }

    @Override
    protected QueryExecution getQueryExecution(Query query) {
        return QueryExecutionFactory.create(query, model);
    }

    @Override
    protected void executeUpdate(UpdateRequest updateRequest) {
        UpdateAction.execute(updateRequest, this.model);
    }
}
