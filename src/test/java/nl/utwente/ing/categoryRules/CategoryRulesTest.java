package nl.utwente.ing.categoryRules;

import io.restassured.path.json.JsonPath;
import jdk.nashorn.internal.parser.JSONParser;
import nl.utwente.ing.Utils;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static nl.utwente.ing.Utils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CategoryRulesTest {

    private static final String CREATE_CATEGORYRULE_JSON_SCHEMA_PATH = "create-categoryRule-response.json";

    @Test
    public void testCreateMissingCategory() {
        String session_id = getValidSessionId();
        JSONObject ruleBody = new JSONObject()
                .put("description", "University of Twente")
                .put("iBAN", "NL39RABO0300065264")
                .put("type", "deposit")
                .put("category_id", 99)
                .put("applyOnHistory", true);
        // Can't create rule with unknown category
        given()
            .header(Utils.X_SESSION_ID_HEADER, session_id)
            .contentType(Utils.APPLICATION_JSON_VALUE)
            .body(ruleBody.toString())
            .when()
            .post(Utils.BASE_URL + Utils.CATEGORY_RULES_PATH)
            .then()
            .assertThat()
            .statusCode(405);
        JSONObject categoryBody = new JSONObject()
                .put("name", "test category");
        int category_id = createCategory(session_id, "test category");
        ruleBody.put("category_id", category_id);
        // now the creation of the rule should succeed, category exists.
        given()
            .header(Utils.X_SESSION_ID_HEADER, session_id)
            .contentType(Utils.APPLICATION_JSON_VALUE)
            .body(ruleBody.toString())
            .when()
            .post(Utils.BASE_URL + Utils.CATEGORY_RULES_PATH)
            .then()
            .assertThat()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath(CREATE_CATEGORYRULE_JSON_SCHEMA_PATH));
    }

    @Test
    public void testMissingProperties() {
        String session_id = getValidSessionId();
        int category_id = createCategory(session_id, "test category");
        JSONObject ruleBody = new JSONObject()
                .put("description", "")
                .put("iBAN", "")
                .put("type", "")
                .put("category_id", category_id)
                .put("applyOnHistory", true);
        // the fields: description, iBAN and type can all be empty to match all transactions.
        given()
            .header(X_SESSION_ID_HEADER, session_id)
            .contentType(Utils.APPLICATION_JSON_VALUE)
            .body(ruleBody.toString())
            .when()
            .post(BASE_URL + CATEGORY_RULES_PATH)
            .then()
            .assertThat()
            .statusCode(201);
    }

    @Test
    public void testGetCategoryRules() {
        String session_id = getValidSessionId();
        JSONObject ruleBody = new JSONObject()
                .put("iBAN", "")
                .put("type", "")
                .put("applyOnHistory", true);
        int[] category_ids = new int[10];
        for (int i=0; i < 10; i++) {
            ruleBody.put("description", "category " + i);
            int category_id = createCategory(session_id, "category " + i);
            category_ids[i] = category_id;
            ruleBody.put("category_id", category_id);
            given()
                .header(X_SESSION_ID_HEADER, session_id)
                .contentType(APPLICATION_JSON_VALUE)
                .body(ruleBody.toString())
                .when()
                .post(BASE_URL + CATEGORY_RULES_PATH)
                .then()
                .assertThat()
                .statusCode(201);
        }
        JsonPath getRules = given()
                                .header(X_SESSION_ID_HEADER, session_id)
                                .when()
                                .get(BASE_URL + CATEGORY_RULES_PATH)
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .extract().jsonPath();
        ArrayList<HashMap<String, String>> rules = getRules.get("$");
        for (int i=0; i < 10; i++) {
            assertEquals("category " + i, rules.get(i).get("description"));
            assertEquals(category_ids[i], rules.get(i).get("category_id"));
        }
    }

    private int createCategory(String session_id, String name) {
        JSONObject categoryBody = new JSONObject()
                .put("name", name);
        return given().
                header(Utils.X_SESSION_ID_HEADER, session_id)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(categoryBody.toString())
                .when()
                .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .extract().path("id");
    }

}
