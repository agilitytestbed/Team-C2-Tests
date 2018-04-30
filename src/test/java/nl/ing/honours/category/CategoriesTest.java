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
import static org.junit.Assert.assertNotEquals;

public class CategoriesTest {
    private static final String ID_NAME = "id";
    private static final String SOME_ID_VALUE = "0";
    private static final String CATEGORY_NAME_NAME = "name";
    private static final String GROCERIES_CATEGORY_NAME_VALUE = "Groceries";
    private static final String TOILETRIES_CATEGORY_NAME_VALUE = "Toiletries";


    private static final String CATEGORY_JSON_SCHEMA_PATH = "category-response.json";

    /*
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
        ArrayList arrayListGet = jsonPathGet.get("$");
        assertEquals(0, arrayListGet.size());
    }

    @Test
    public void testGetAfterPost() {
        String sessionId = Utils.getValidSessionId();

        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
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

        HashMap categoryPost = jsonPathPost.get("$");
        ArrayList arrayListGet = jsonPathGet.get("$");
        assertEquals(1, arrayListGet.size());
        HashMap categoryGet = (HashMap) arrayListGet.get(0);
        assertEquals(categoryPost, categoryGet);
    }

    /*
     * tests for POST /categories
     * For authentication related tests please see "SessionsTest"
     */
    @Test
    public void testPostWithId() {
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(ID_NAME, SOME_ID_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testPostWithName() {
        JsonPath jsonPath = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(CATEGORY_JSON_SCHEMA_PATH))
                .extract().jsonPath();
        HashMap postCategory = jsonPath.get("$");
        assertEquals(GROCERIES_CATEGORY_NAME_VALUE, postCategory.get(CATEGORY_NAME_NAME));
    }

    @Test
    public void testPostWithEmptyBody() {
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(Utils.EMPTY_BODY)
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testPostWithIdAndName() {
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).put(ID_NAME, SOME_ID_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testPostWithDuplicateName() {
        String sessionId = Utils.getValidSessionId();
        JsonPath jsonPathPostOne = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(CATEGORY_JSON_SCHEMA_PATH))
                .extract().jsonPath();

        JsonPath jsonPathPostTwo = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    /*
     * tests for GET /categories/{categoryId}
     * For authentication related tests please see "SessionsTest"
     */
    @Test
    public void testGetByIdAfterPost() {
        String sessionId = Utils.getValidSessionId();
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap categoryPost = jsonPathPost.get("$");

        JsonPath jsonPathGet = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .when()
                .get(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + categoryPost.get(ID_NAME))
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap categoryGet = jsonPathGet.get("$");
        assertEquals(categoryGet, categoryPost);
    }

    @Test
    public void testGetByInvalidId() {
        String invalidId = "this_is_not_an_integer";
        JsonPath jsonPathGet = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .when()
                .get(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + invalidId)
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testGetByUnknownId() {
        String unknownId = "12345678";
        JsonPath jsonPathGet = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .when()
                .get(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + unknownId)
                .then()
                .assertThat()
                .statusCode(404)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    /*
     * tests for PUT /categories/{categoryId}
     * For authentication related tests please see "SessionsTest"
     */
    @Test
    public void testPutByInvalidId() {
        String invalidId = "this_is_not_an_integer";
        JsonPath jsonPathPut = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .put(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + invalidId)
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testPutByUnknownId() {
        String unknownId = "12345678";
        JsonPath jsonPathPut = given()
                .header(Utils.X_SESSION_ID_HEADER, Utils.getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .put(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + unknownId)
                .then()
                .assertThat()
                .statusCode(404)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testPutWithEmptyBodyAfterPost() {
        String sessionId = Utils.getValidSessionId();
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap categoryPost = jsonPathPost.get("$");

        JsonPath jsonPathPut = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body("")
                .when()
                .put(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + categoryPost.get(ID_NAME))
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }

    @Test
    public void testGetAfterPutAfterPost() {
        String sessionId = Utils.getValidSessionId();
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap categoryPost = jsonPathPost.get("$");

        JsonPath jsonPathPut = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, TOILETRIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .put(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + categoryPost.get(ID_NAME))
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        JsonPath jsonPathGet = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .when()
                .get(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + categoryPost.get(ID_NAME))
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap categoryPut = jsonPathPut.get("$");
        HashMap categoryGet = jsonPathGet.get("$");

        assertEquals(categoryPut.get(ID_NAME), categoryPost.get(ID_NAME));
        assertEquals(categoryGet.get(ID_NAME), categoryPut.get(ID_NAME));
        assertNotEquals(categoryGet.get(CATEGORY_NAME_NAME), categoryPost.get(CATEGORY_NAME_NAME));
        assertEquals(categoryGet.get(CATEGORY_NAME_NAME), categoryPut.get(CATEGORY_NAME_NAME));
    }

    @Test
    public void testPutWithIdInBody() {
        String sessionId = Utils.getValidSessionId();
        JsonPath jsonPathPost = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(CATEGORY_NAME_NAME, GROCERIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();

        HashMap categoryPost = jsonPathPost.get("$");

        JsonPath jsonPathPut = given()
                .header(Utils.X_SESSION_ID_HEADER, sessionId)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(new JSONObject().put(ID_NAME, categoryPost.get(ID_NAME)).put(CATEGORY_NAME_NAME, TOILETRIES_CATEGORY_NAME_VALUE).toString())
                .when()
                .put(Utils.BASE_URL + Utils.CATEGORIES_PATH + "/" + categoryPost.get(ID_NAME))
                .then()
                .assertThat()
                .statusCode(405)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .extract().jsonPath();
    }
}
