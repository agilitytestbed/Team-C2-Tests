package nl.ing.honours.sessions;

import static io.restassured.RestAssured.when;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class SessionsTest {
    public static void main(String[] args) {
        when().
                post("/api/v1/sessions").
        then().assertThat().
                statusCode(200).
                contentType("application/json").
                body(matchesJsonSchemaInClasspath("session-response.json"));
    }

}
