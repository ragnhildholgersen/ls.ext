package no.deichman.services.search;

import no.deichman.services.entity.ResourceBase;
import no.deichman.services.uridefaults.XURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static no.deichman.services.entity.EntityType.ALL_TYPES_PATTERN;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Responsibility: Expose a subset of Elasticsearch REST API limited to searching for works, as well as providing
 * routes to trigger reindexing of resources.
 */
@Singleton
@Path("search")
public class SearchResource extends ResourceBase {
    private static final int NUMTHREADS = 3;
    private static final ForkJoinPool THREADPOOL = new ForkJoinPool(NUMTHREADS);
    private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);
    @Context
    private ServletConfig servletConfig;

    public SearchResource() {
    }

    public SearchResource(SearchService searchService) {
        setSearchService(searchService);
    }

    @GET
    @Path("{type: "+ ALL_TYPES_PATTERN + "}/_search")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response search(@PathParam("type") String type, @QueryParam("q") String query) {
        if (isBlank(query)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        switch (type) {
            case "work":
                return getSearchService().searchWork(query);
            case "person":
                return getSearchService().searchPerson(query);
            case "place":
                return getSearchService().searchPlace(query);
            case "corporation":
                return getSearchService().searchCorporation(query);
            case "serial":
                return getSearchService().searchSerial(query);
            case "subject":
                return getSearchService().searchSubject(query);
            case "genre":
                return getSearchService().searchGenre(query);
            case "publication":
                return getSearchService().searchPublication(query);
            case "instrument":
                return getSearchService().searchInstrument(query);
            case "compositiontype":
                return getSearchService().searchCompositionType(query);
            case "event":
                return getSearchService().searchEvent(query);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    @POST
    @Path("{type: " + ALL_TYPES_PATTERN + "}/_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response searchJson(String body, @PathParam("type") String type, @Context UriInfo uriInfo) {
        if (isBlank(body)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        switch (type) {
            case "work":
                return getSearchService().searchWorkWithJson(body, queryParams);
            case "person":
                return getSearchService().searchPersonWithJson(body);
            case "place":
                return getSearchService().searchPlace(body);
            case "corporation":
                return getSearchService().searchCorporation(body);
            case "serial":
                return getSearchService().searchSerial(body);
            case "subject":
                return getSearchService().searchSubject(body);
            case "genre":
                return getSearchService().searchGenre(body);
            case "publication":
                return getSearchService().searchPublicationWithJson(body);
            default:
                throw new RuntimeException("Unknown type: " + type);
        }
    }

    @POST
    @Path("clear_index")
    public final Response clearIndex() {
        return getSearchService().clearIndex();
    }

    @POST
    @Path("{type: "+ ALL_TYPES_PATTERN + "}/reindex_all")
    public final Response reIndex(@PathParam("type") final String type) {
        THREADPOOL.execute(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting to reindex " + type);
                long start = System.currentTimeMillis();
                getEntityService().retrieveAllWorkUris(type, uri -> CompletableFuture.runAsync(() -> {
                    try {
                        getSearchService().index(new XURI(uri));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
                LOG.info("Done reindexing " + type + " in " + (System.currentTimeMillis() - start) + " seconds");
            }
        });
        return Response.accepted().build();
    }


    @POST
    @Path("work/reindex")
    public final Response reIndexWorkByRecordId(
            @QueryParam("recordId") final String recordId,
            @QueryParam("branches") final String branches) throws Exception {

        if (recordId == null || branches == null) {
            throw new BadRequestException("missing one or more of required queryparameters: recordId, branches");
        }

        XURI workUri = getEntityService().updateHoldingBranches(recordId, branches);

        try {
            getSearchService().index(workUri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.accepted().build();
    }


    @Override
    protected final ServletConfig getConfig() {
        return servletConfig;
    }
}
