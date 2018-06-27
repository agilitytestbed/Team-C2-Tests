package nl.utwente.ing.transaction;

import io.restassured.path.json.JsonPath;
import nl.utwente.ing.Utils;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static nl.utwente.ing.Utils.getValidSessionId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TransactionsTest {

    private static final String CREATE_TRANSACTION_JSON_SCHEMA_PATH = "create-transaction-response.json";
    private static final String GET_TRANSACTION_JSON_SCHEMA_PATH = "get-transaction-response.json";

    @Test
    public void createEmptyTransaction() {
        // empty requestBody should return status code 405
        JSONObject requestBody = new JSONObject();
        given()
                .header(Utils.X_SESSION_ID_HEADER, getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(requestBody.toString())
                .when()
                .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then().assertThat()
                .statusCode(405);
    }

    @Test
    public void createTransactionInvalidAttribute() {
        // invalid value type for amount (string should be float),
        // should return status code 405
        JSONObject requestBody = new JSONObject()
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("externalIBAN", "NL39RABO0300065264")
                .put("amount", "€500")
                .put("type", "deposit");
        given()
                .header(Utils.X_SESSION_ID_HEADER, getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(requestBody.toString())
                .when()
                .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then().assertThat()
                .statusCode(405);
    }

    @Test
    public void createTransactionMissingAttribute() {
        // missing attribute type
        // should return status code 405
        JSONObject requestBody = new JSONObject()
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("externalIBAN", "NL39RABO0300065264")
                .put("amount", 213.12);
        given()
                .header(Utils.X_SESSION_ID_HEADER, getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(requestBody.toString())
                .when()
                .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then().assertThat()
                .statusCode(405);
    }

    @Test
    public void createValidTransaction() {
        JSONObject requestBody = new JSONObject()
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("externalIBAN", "NL39RABO0300065264")
                .put("amount", 500)
                .put("type", "deposit");
        given()
                .header(Utils.X_SESSION_ID_HEADER, getValidSessionId())
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(requestBody.toString())
                .when()
                .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then().assertThat()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath(CREATE_TRANSACTION_JSON_SCHEMA_PATH));
    }

    @Test
    public void getEmptyTransactionList() {
        given()
                .header(Utils.X_SESSION_ID_HEADER, getValidSessionId())
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then().assertThat()
                .statusCode(200)
                .body("", Matchers.hasSize(0));
    }

    @Test
    public void getTransactionAfterPost() {
        String session_id = getValidSessionId();
        JSONObject requestBody = new JSONObject()
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("externalIBAN", "NL39RABO0300065264")
                .put("amount", 500)
                .put("type", "deposit");

        JsonPath postPath = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(requestBody.toString())
                .when()
                .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then()
                .assertThat()
                .statusCode(201)
                .extract().jsonPath();

        JsonPath getPath = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(GET_TRANSACTION_JSON_SCHEMA_PATH))
                .extract().jsonPath();

        assertEquals("The length of the transaction list is not equal to 1",
                1 ,((ArrayList) getPath.get()).size());
        HashMap postTransaction = postPath.get("$");
        HashMap getTransaction = getPath.get("[0]");
        assertEquals(postTransaction, getTransaction);
    }

    @Test
    public void testGetTransactionsUsingCategory() {
        String session_id = getValidSessionId();
        createTestTransactions(session_id);

        JsonPath getResult = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                .then()
                .assertThat()
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(GET_TRANSACTION_JSON_SCHEMA_PATH))
                .extract().jsonPath();
        assertEquals("The length of the transaction list is not equal to 10",10,
                ((ArrayList)getResult.get("$")).size());

        JsonPath getCat0 = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "?category=category0")
                .then()
                .assertThat()
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(GET_TRANSACTION_JSON_SCHEMA_PATH))
                .extract().jsonPath();
        assertEquals(((ArrayList) getCat0.get("$")).size(), 5);

        JsonPath getCat1 = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "?category=category0")
                .then()
                .assertThat()
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(matchesJsonSchemaInClasspath(GET_TRANSACTION_JSON_SCHEMA_PATH))
                .extract().jsonPath();
        assertEquals(((ArrayList) getCat1.get("$")).size(), 5);
    }

    @Test
    public void testGetTransactionsUsingOffsetAndLimit() {
        String session_id = getValidSessionId();
        HashMap<String, List<Integer>> createdIds = createTestTransactions(session_id);

        for (int offset = 0; offset < createdIds.get("transactions").size(); offset++) {
            JsonPath getWithOffset = given()
                    .header(Utils.X_SESSION_ID_HEADER, session_id)
                    .when()
                    .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "?offset=" + offset + "&limit=100")
                    .then()
                    .assertThat()
                    .contentType(Utils.APPLICATION_JSON_VALUE)
                    .body(matchesJsonSchemaInClasspath(GET_TRANSACTION_JSON_SCHEMA_PATH))
                    .extract().jsonPath();
            assertEquals("Length of the transaction list with offset " + offset + " invalid.",10 - offset,
                    ((ArrayList)getWithOffset.get("$")).size());
            for (int limit = 10; limit >= 0; limit--) {
                JsonPath getWithOffsetAndLimit = given()
                        .header(Utils.X_SESSION_ID_HEADER, session_id)
                        .when()
                        .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "?offset=" + offset + "&limit=" + limit)
                        .then()
                        .assertThat()
                        .contentType(Utils.APPLICATION_JSON_VALUE)
                        .body(matchesJsonSchemaInClasspath(GET_TRANSACTION_JSON_SCHEMA_PATH))
                        .extract().jsonPath();
                if ((10 - offset) < limit) {
                    assertEquals("Length of the transaction list with offset " + offset + " and limit " + limit + " invalid.",
                            10 - offset,
                            ((ArrayList)getWithOffsetAndLimit.get("$")).size());
                } else {
                    assertEquals("Length of the transaction list with offset " + offset + " and limit " + limit + " invalid.",
                            limit,
                            ((ArrayList)getWithOffsetAndLimit.get("$")).size());
                }

            }
        }
    }

    @Test
    public void testFindTransaction() {
        String session_id = getValidSessionId();
        HashMap<String, List<Integer>> createdIds = createTestTransactions(session_id);
        for (Integer id : createdIds.get("transactions")) {
            given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + id)
                .then()
                .assertThat()
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(CREATE_TRANSACTION_JSON_SCHEMA_PATH));
        }
    }

    @Test
    public void testUpdateTransaction() {
        String session_id = getValidSessionId();
        HashMap<String, List<Integer>> createdIds = createTestTransactions(session_id);
        int firstId = createdIds.get("transactions").get(0);
        int randomAmount = new Random().nextInt(100);
        JSONObject updatedTransactionBody = new JSONObject()
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("externalIBAN", "NL39RABO0300065264")
                .put("amount", randomAmount)
                .put("type", "deposit");
        JsonPath updatedTransaction = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .body(updatedTransactionBody.toString())
                .when()
                .put(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + firstId)
                .then()
                .assertThat()
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(CREATE_TRANSACTION_JSON_SCHEMA_PATH))
                .extract().jsonPath();
        int resultAmount = Math.round(updatedTransaction.get("amount"));
        assertEquals(randomAmount, resultAmount);
    }

    @Test
    public void testDeleteTransaction() {
        String session_id = getValidSessionId();
        HashMap<String, List<Integer>> createdIds = createTestTransactions(session_id);
        int firstId = createdIds.get("transactions").get(0);
        // delete first transaction
        given()
            .header(Utils.X_SESSION_ID_HEADER, session_id)
            .when()
            .delete(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + firstId)
            .then()
            .assertThat()
            .statusCode(204);

        // search for transaction in transaction list
        JsonPath getTransactions = given().
                        header(Utils.X_SESSION_ID_HEADER, session_id).
                        when().
                        get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                        .then()
                        .statusCode(200)
                        .extract().jsonPath();
        ArrayList<HashMap<String, Object>> transactionList = getTransactions.get();
        for (HashMap<String, Object> transaction : transactionList) {
            if ((int) transaction.get("id") == firstId) {
                fail("Deleted transaction found using getTransaction");
            }
        }
        // find transaction
        given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + firstId)
                .then()
                .log().body()
                .assertThat()
                .statusCode(404);
    }

    @Test
    public void testAssignCategory() {
        String session_id = getValidSessionId();
        HashMap<String, List<Integer>> createdIds = createTestTransactions(session_id);
        int firstId = createdIds.get("transactions").get(0);
        String assign_category_url = Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + firstId + "/" + "category";
        // create new category
        JSONObject categoryBody = new JSONObject()
                .put("name", "newCategory");
        int category_id =
                given().
                        header(Utils.X_SESSION_ID_HEADER, session_id).
                        contentType(Utils.APPLICATION_JSON_VALUE).
                        body(categoryBody.toString()).
                        when().
                        post(Utils.BASE_URL + Utils.CATEGORIES_PATH).
                        then().
                        extract().path("id");
        // assign new category to first transaction
        JSONObject assignBody = new JSONObject()
                .put("category_id", category_id);
        given().
                header(Utils.X_SESSION_ID_HEADER, session_id).
                contentType(Utils.APPLICATION_JSON_VALUE).
                body(assignBody.toString()).
                when().
                patch(assign_category_url).
                then().assertThat().
                statusCode(200);
        // verify new category assignment
        JsonPath firstTransaction = given()
                .header(Utils.X_SESSION_ID_HEADER, session_id)
                .when()
                .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + firstId)
                .then()
                .assertThat()
                .contentType(Utils.APPLICATION_JSON_VALUE)
                .statusCode(200)
                .extract().jsonPath();
        HashMap<String, Object> category = firstTransaction.get("category");
        assertEquals("Failed to assign new category to transaction.", category.get("name"), "newCategory");
        assertEquals("Failed to assign new category to transaction.", category.get("id"), category_id);
    }

    private HashMap<String, List<Integer>> createTestTransactions(String session_id) {
        ArrayList<Integer> transactionIds = new ArrayList<>();
        ArrayList<Integer> categoryIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            // create category
            JSONObject categoryBody = new JSONObject()
                    .put("name", "category" + i % 2);
            int category_id =
                    given()
                            .header(Utils.X_SESSION_ID_HEADER, session_id)
                            .contentType(Utils.APPLICATION_JSON_VALUE)
                            .body(categoryBody.toString())
                            .when()
                            .post(Utils.BASE_URL + Utils.CATEGORIES_PATH)
                            .then()
                            .extract().path("id");
            categoryIds.add(category_id);
        }
        for (int i = 0; i < 10; i++) {
            JSONObject transactionBody = new JSONObject()
                    .put("date", "2018-04-12T12:28:34.642Z")
                    .put("externalIBAN", "NL39RABO0300065264")
                    .put("amount", i)
                    .put("type", "deposit");
            // create transaction
            int transaction_id =
                    given()
                            .header(Utils.X_SESSION_ID_HEADER, session_id)
                            .contentType(Utils.APPLICATION_JSON_VALUE)
                            .body(transactionBody.toString())
                            .when()
                            .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                            .then()
                            .extract().path("id");
            transactionIds.add(transaction_id);
            // assign category
            String assign_category_url = Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + transaction_id + "/" + "category";
            JSONObject assignBody = new JSONObject()
                    .put("category_id", categoryIds.get(i % 2));
            given()
                    .header(Utils.X_SESSION_ID_HEADER, session_id)
                    .contentType(Utils.APPLICATION_JSON_VALUE)
                    .body(assignBody.toString())
                    .when()
                    .patch(assign_category_url)
                    .then().assertThat()
                    .statusCode(200);
        }
        HashMap<String, List<Integer>> resultMap = new HashMap<>();
        resultMap.put("transactions", transactionIds);
        resultMap.put("categories", categoryIds);
        return resultMap;
    }

    @Test
    public void testDescriptions() {
        String session_id = getValidSessionId();
        JSONObject transactionBody = new JSONObject()
                .put("date", "2018-04-12T12:28:34.642Z")
                .put("externalIBAN", "NL39RABO0300065264")
                .put("description", "This is a test description!")
                .put("amount", "200")
                .put("type", "deposit");
        int transaction_id = given()
                                .header(Utils.X_SESSION_ID_HEADER, session_id)
                                .contentType(Utils.APPLICATION_JSON_VALUE)
                                .body(transactionBody.toString())
                                .when()
                                .post(Utils.BASE_URL + Utils.TRANSACTIONS_PATH)
                                .then()
                                .extract().path("id");
        String transactionDescription = given()
                                            .header(Utils.X_SESSION_ID_HEADER, session_id)
                                            .when()
                                            .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + transaction_id)
                                            .then()
                                            .extract().path("description");
        assertEquals("This is a test description!", transactionDescription);
        transactionBody.put("description", "This is different description!");
        given()
            .header(Utils.X_SESSION_ID_HEADER, session_id)
            .contentType(Utils.APPLICATION_JSON_VALUE)
            .body(transactionBody.toString())
            .when()
            .put(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + transaction_id)
            .then()
            .assertThat()
            .statusCode(200);
        String changedTransactionDescription = given()
                                                    .header(Utils.X_SESSION_ID_HEADER, session_id)
                                                    .when()
                                                    .get(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + transaction_id)
                                                    .then()
                                                    .extract().path("description");
        assertEquals("This is a test description!", changedTransactionDescription);
    }
}
