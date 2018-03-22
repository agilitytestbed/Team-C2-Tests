package nl.ing.honours.categories;

import io.restassured.response.Response;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.hasItems;


public class CategoriesTest {
    public static void main(String[] args) {
        Response response = get("/sessions");
        get("/categories").then().headers("WWW_Authenticate", "sessionId").body("lotto.winners.winnerId", hasItems(23, 54));
    }

}
