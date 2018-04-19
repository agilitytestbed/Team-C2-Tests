package nl.ing.honours.category;

import io.restassured.path.json.JsonPath;
import nl.ing.honours.Utils;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.junit.Assert.assertEquals;

public class CategoriesTest {
    private static final String ID_NAME = "id";
    private static final String SOME_ID_VALUE = "0";
    private static final String CATEGORY_NAME_NAME = "name";
    private static final String SOME_CATEGORY_NAME_VALUE = "Groceries";

    private static final String CATEGORY_JSON_SCHEMA_PATH = "category-response.json";

    /**
     * tests for GET /categories
     * For authentication related tests please see "SessionsTest"
     */
    @Test
    public void testGet() {
        JsonPath jsonPathGet = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .when()
                .get(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
        ArrayList getArrayList = jsonPathGet.get("$");
        assertEquals(0, getArrayList.size());
    }

    @Test
    public void testGetAfterPost() {
        String sessionId = Utils.getValidSessionId();

        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, SOME_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        JsonPath jsonPathGet = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .when()
                .get(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap postCategory = jsonPathPost.get("$");
        ArrayList getArrayList = jsonPathGet.get("$");
        assertEquals(1, getArrayList.size());
        HashMap getCategory = (HashMap) getArrayList.get(0);
        assertEquals(postCategory, getCategory);
    }

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
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE);
    }

    @Test
    public void testPostWithName() {
        JsonPath jsonPath = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, SOME_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(CATEGORY_JSON_SCHEMA_PATH))
                .extract().jsonPath();
        HashMap postCategory = jsonPath.get("$");
        assertEquals(SOME_CATEGORY_NAME_VALUE, postCategory.get(CATEGORY_NAME_NAME));
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
                .assertThat()
                .statusCode(405);
    }

    @Test
    public void testPostWithIdAndName() {
        given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, SOME_CATEGORY_NAME_VALUE).put(ID_NAME, SOME_ID_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(405);
    }

    @Test
    public void testPostWithDuplicateName() {
        String sessionId = Utils.getValidSessionId();
        given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, SOME_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH);

        given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, SOME_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(405);
    }
}
