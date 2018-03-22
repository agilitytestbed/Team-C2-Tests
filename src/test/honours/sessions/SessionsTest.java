package honours.sessions;

import org.junit.Test;

import static io.restassured.RestAssured.when;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class SessionsTest {
    
    @Test
    public void getSessionTest() {
        when().
                post("/api/v1/sessions").
        then().assertThat().
                statusCode(200).
                contentType("application/json").
                body(matchesJsonSchemaInClasspath("session-response.json"));
    }

}
