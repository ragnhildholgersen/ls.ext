package no.deichman.services.entity.z3950;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.deichman.services.utils.ResourceReader;
import org.junit.Test;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


/**
 * Responsibility: test mapper class.
 */
public class MapperTest extends MappingTester {

    @Test
    public void test_default_constructor() {
        assertNotNull(new Mapper());
    }

    @Test
    public void test_mapping() throws Exception {
        ResourceReader resourceReader = new ResourceReader();

        String sourceName = "bibbi";
        String record = resourceReader.readFile("BS_external_data.xml");
        String json = resourceReader.readFile("BS_external_data_processed.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Mapper mapper = new Mapper();

        assertEquals(simplifyBNodes(json), simplifyBNodes(gson.toJson(mapper.map(sourceName, record))));
        assertThat(simplifyBNodes(json),
                SameJSONAs.sameJSONAs(simplifyBNodes(gson.toJson(mapper.map(sourceName, record))))
                        .allowingAnyArrayOrdering()
                        .allowingExtraUnexpectedFields());
        assertEquals(sourceName, mapper.map(sourceName, record).getSource());
    }

    @Test(expected = Exception.class)
    public void mapper_throws_exception_on_unknown_format() throws Exception {
        Mapper mapper = new Mapper();
        mapper.map("google", null);
    }
}
