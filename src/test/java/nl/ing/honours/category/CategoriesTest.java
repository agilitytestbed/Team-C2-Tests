package nl.ing.honours.category;

import nl.ing.honours.Utils;
import org.json.JSONObject;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class CategoriesTest {
    private static final String GROCERIES_CATEGORY_NAME_VALUE = "Groceries";
    private static final String ID_NAME = "id";
    private static final String CATEGORY_NAME = "name";
    private static final String SOME_ID_VALUE = "0";
    private static final String CATEGORY_JSON_SCHEMA_PATH = "category-response.json";

    /**
     * tests for POST /categories
     * For authentication related tests please see "SessionsTest"
     */
    @Test
    public void testPostWithId() {
        given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(ID_NAME, SOME_ID_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat().statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE);
    }

    @Test
    public void testPostWithName() {
        given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(CATEGORY_JSON_SCHEMA_PATH));
    }

    @Test
    public void testPostWithEmptyBody() {
        given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(Utils.EMPTY_BODY)
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat().statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE);
    }

    @Test
    public void testPostWithIdAndName() {
        given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME, GROCERIES_CATEGORY_NAME_VALUE).put(ID_NAME, SOME_ID_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat().statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE);
    }

    @Test
    public void testPostWithDuplicateName() {
        String sessionId = Utils.getValidSessionId();
        given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH);

        given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat().statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE);
    }
}
