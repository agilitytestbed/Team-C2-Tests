package nl.ing.honours.sessions;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.hasItems;

public class SessionsTest {
    public static void main(String[] args) {
        when().
                get("/sessions").
                then().
                body("lotto.winners.winnerId", hasItems(23, 54));
    }
}
