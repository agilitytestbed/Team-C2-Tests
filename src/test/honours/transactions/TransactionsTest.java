package honours.transactions;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

public class TransactionsTest {

    private static final String BASE_URL = "/api/v1";
    private static final String sessionID = obtainSession();

    @Test
    public void createInvalidTransaction() {
        Map<String, Object> requestBody = new HashMap<>();
        // empty requestBody
        given()
                .header("X-session-ID", sessionID).
        when().
                post(BASE_URL + "/transactions").
        then().assertThat().
                statusCode(405);


    }

    private static String obtainSession() {
        return when().
                    post(BASE_URL + "/sessions").
                then().assertThat().
                    statusCode(201).
                extract().
                    path("id");
    }
}
