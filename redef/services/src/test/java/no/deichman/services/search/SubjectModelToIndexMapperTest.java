package no.deichman.services.search;

import no.deichman.services.entity.EntityType;
import no.deichman.services.uridefaults.BaseURI;
import no.deichman.services.uridefaults.XURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;

import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

/**
 * Responsibility: unit test PlaceModelToIndexMapper.
 */
public class SubjectModelToIndexMapperTest {

    @Test
    public void testModelToIndexDocument() throws Exception {
        XURI subjetXuri1 = new XURI(BaseURI.root(), EntityType.SUBJECT.getPath(), "e000000001");
        Model model = ModelFactory.createDefaultModel();
        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource(subjetXuri1.getUri()),
                RDF.type,
                ResourceFactory.createResource(BaseURI.ontology("Subject"))));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource(subjetXuri1.getUri()),
                ResourceFactory.createProperty(BaseURI.ontology() + "specification"),
                ResourceFactory.createPlainLiteral("Måte å lage klesplagg ved hjelp av to pinner og garn")));

        model.add(ResourceFactory.createStatement(
                ResourceFactory.createResource(subjetXuri1.getUri()),
                ResourceFactory.createProperty(BaseURI.ontology("prefLabel")),
                ResourceFactory.createPlainLiteral("Strikking")));

        String jsonDocument = new ModelToIndexMapper("subject").createIndexDocument(model, subjetXuri1);

        Assert.assertThat(jsonDocument, sameJSONAs(""
                + "{"
                + "  \"uri\": \"" + subjetXuri1.getUri() + "\","
                + "  \"prefLabel\": \"Strikking\","
                + "  \"specification\": \"Måte å lage klesplagg ved hjelp av to pinner og garn\","
                + "}").allowingAnyArrayOrdering());
    }
}
