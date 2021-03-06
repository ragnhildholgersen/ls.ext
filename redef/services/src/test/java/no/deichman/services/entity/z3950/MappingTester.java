package no.deichman.services.entity.z3950;

/**
 * Responsibility: provide tools for testing mapped objects.
 */
public class MappingTester {
    public final String simplifyBNodes(String data) {
        return data.replaceAll("\"_:[^\"]+\"", "\"_:0000000\"");
    }
}
