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

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static nl.utwente.ing.Utils.*;
import static org.junit.Assert.assertEquals;

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
        createCategoryRule(session_id, ruleBody);
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
            createCategoryRule(session_id, ruleBody);
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

    @Test
    public void testDescriptionSubstring() {
        String session_id = getValidSessionId();
        int cat_id = createCategory(session_id, "test_category");
        JSONObject ruleBody = new JSONObject()
                .put("description", "Test")
                .put("iBAN", "")
                .put("type", "")
                .put("category_id", cat_id)
                .put("applyOnHistory", false);
        // create category rule
        createCategoryRule(session_id, ruleBody);
        JSONObject transactionBody = new JSONObject()
                .put("description", "Test Description 123")
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("amount", 500)
                .put("externalIBAN", "NL39RABO0300065264")
                .put("type", "deposit");
        // create transaction with description that matches the category rule.
        int transaction_id = given()
                                .header(X_SESSION_ID_HEADER, session_id)
                                .contentType(APPLICATION_JSON_VALUE)
                                .body(transactionBody.toString())
                                .when()
                                .post(BASE_URL + TRANSACTIONS_PATH)
                                .then()
                                .assertThat()
                                .statusCode(201)
                                .extract().path("id");
        // retrieve transaction and check assigned category.
        JsonPath foundTransaction = given()
                                        .header(X_SESSION_ID_HEADER, session_id)
                                        .when()
                                        .get(BASE_URL + TRANSACTIONS_PATH + "/" + transaction_id)
                                        .then()
                                        .assertThat()
                                        .statusCode(200)
                                        .extract().jsonPath();
        HashMap<String, Object> category = foundTransaction.get("category");
        assertEquals(cat_id, category.get("id"));
    }

    @Test
    public void testPartialMatch() {
        String session_id = getValidSessionId();
        int cat_id = createCategory(session_id, "partial_test");
        JSONObject ruleBody = new JSONObject()
                .put("description", "Test")
                .put("iBAN", "1234")
                .put("type", "")
                .put("category_id", cat_id)
                .put("applyOnHistory", false);
        // create category rule
        createCategoryRule(session_id, ruleBody);
        JSONObject transactionBody = new JSONObject()
                .put("description", "Test description")
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("amount", 500)
                .put("externalIBAN", "NL39RABO0300065264")
                .put("type", "deposit");
        // create transaction with description that matches the category rule and IBAN that doesn't match.
        int transaction_id = given()
                .header(X_SESSION_ID_HEADER, session_id)
                .contentType(APPLICATION_JSON_VALUE)
                .body(transactionBody.toString())
                .when()
                .post(BASE_URL + TRANSACTIONS_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .extract().path("id");
        // retrieve transaction and check that category isn't assigned.
        JsonPath foundTransaction = given()
                .header(X_SESSION_ID_HEADER, session_id)
                .when()
                .get(BASE_URL + TRANSACTIONS_PATH + "/" + transaction_id)
                .then()
                .assertThat()
                .statusCode(200)
                .extract().jsonPath();
        HashMap<String, Object> category = foundTransaction.get("category");
        assertEquals(null, category);
    }

    @Test
    public void testApplicationOrder() {
        String session_id = getValidSessionId();
        int cat_id1 = createCategory(session_id, "application_order1");
        int cat_id2 = createCategory(session_id, "application_order2");
        JSONObject ruleBody1 = new JSONObject()
                .put("description", "Test123")
                .put("iBAN", "1234")
                .put("type", "")
                .put("category_id", cat_id1)
                .put("applyOnHistory", false);
        JSONObject ruleBody2 = new JSONObject()
                .put("description", "Test123")
                .put("iBAN", "23")
                .put("type", "deposit")
                .put("category_id", cat_id2)
                .put("applyOnHistory", false);
        // create both category rules.
        createCategoryRule(session_id, ruleBody1);
        createCategoryRule(session_id, ruleBody2);
        JSONObject transactionBody = new JSONObject()
                .put("description", "Test desTest123cription")
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("amount", 500)
                .put("externalIBAN", "NL39RABO0312345264")
                .put("type", "deposit");
        // create transaction that matches both rules.
        int transaction_id = given()
                .header(X_SESSION_ID_HEADER, session_id)
                .contentType(APPLICATION_JSON_VALUE)
                .body(transactionBody.toString())
                .when()
                .post(BASE_URL + TRANSACTIONS_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .extract().path("id");
        // retrieve transaction and check that category the oldest category rule is assigned.
        JsonPath foundTransaction = given()
                .header(X_SESSION_ID_HEADER, session_id)
                .when()
                .get(BASE_URL + TRANSACTIONS_PATH + "/" + transaction_id)
                .then()
                .assertThat()
                .statusCode(200)
                .extract().jsonPath();
        HashMap<String, Object> category = foundTransaction.get("category");
        assertEquals(cat_id1, category.get("id"));
    }

    @Test
    public void testApplyOnHistory() {
        String session_id = getValidSessionId();
        JSONObject transactionBody = new JSONObject()
                .put("description", "Test desTest123cription")
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("amount", 500)
                .put("externalIBAN", "NL39RABO0312345264")
                .put("type", "withdrawal");
        // create transaction
        int transaction_id = given()
                                .header(X_SESSION_ID_HEADER, session_id)
                                .contentType(APPLICATION_JSON_VALUE)
                                .body(transactionBody.toString())
                                .when()
                                .post(BASE_URL + TRANSACTIONS_PATH)
                                .then()
                                .assertThat()
                                .statusCode(201)
                                .extract().path("id");
        int cat_id = createCategory(session_id, "applyOnHistory");
        JSONObject ruleBody = new JSONObject()
                .put("description", "Test123")
                .put("iBAN", "1234")
                .put("type", "")
                .put("category_id", cat_id)
                .put("applyOnHistory", true);
        // create category rule
        createCategoryRule(session_id, ruleBody);
        // retrieve transaction and check if the category rule is applied on history
        JsonPath foundTransaction = given()
                                        .header(X_SESSION_ID_HEADER, session_id)
                                        .when()
                                        .get(BASE_URL + TRANSACTIONS_PATH + "/" + transaction_id)
                                        .then()
                                        .assertThat()
                                        .statusCode(200)
                                        .extract().jsonPath();
        HashMap<String, Object> category = foundTransaction.get("category");
        assertEquals(cat_id, category.get("id"));
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

    private int createCategoryRule(String session_id, JSONObject body) {
        return given()
                .header(X_SESSION_ID_HEADER, session_id)
                .contentType(APPLICATION_JSON_VALUE)
                .body(body.toString())
                .when()
                .post(BASE_URL + CATEGORY_RULES_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .extract().path("id");
    }

}
