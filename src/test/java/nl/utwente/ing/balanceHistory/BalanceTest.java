package nl.utwente.ing.balanceHistory;

import nl.utwente.ing.Utils;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static nl.utwente.ing.Utils.*;
import static org.junit.Assert.assertEquals;

public class BalanceTest {

    @Test
    public void testMixedTransactions() {
        String session = getValidSessionId();
        JSONObject transactionBody = new JSONObject()
                .put("externalIBAN", "NL39RABO0300065264");
        // 800-500+350-299+80-160+430-699+2569-123+321 = 2769
        Float finalBalance = 2769f;
        List<Integer> amounts = Arrays.asList(800,500,350,299,80,160,430,699,2569,123,321);
        String type = "deposit";
        for (int amount : amounts) {
            transactionBody.put("amount", amount);
            transactionBody.put("type", type);
            if (type.equals("deposit")) {
                type = "withdrawal";
            } else {
                type = "deposit";
            }
            createTransaction(session, transactionBody);
        }
        assertEquals(finalBalance, getBalance(session));
    }

    @Test
    public void testUpdatingTransactions() {
        String session = getValidSessionId();
        JSONObject transactionBody = new JSONObject()
                .put("externalIBAN", "NL39RABO0300065264");
        // 250-39+175-299+930-89+430-199+1743-987+789 = 2704
        Float finalBalance = 2704f;
        List<Integer> amounts = Arrays.asList(250,39,175,299,930,89,430,199,1743,987,789);
        int[] transaction_ids = new int[11];
        String type = "deposit";
        for (int i = 0; i < 11; i++) {
            transactionBody.put("amount", amounts.get(i));
            transactionBody.put("type", type);
            if (type.equals("deposit")) {
                type = "withdrawal";
            } else {
                type = "deposit";
            }
            transaction_ids[i] = createTransaction(session, transactionBody);
        }
        assertEquals(finalBalance, getBalance(session));
        // change withdrawal 299 to 200, balance + 99
        editTransaction(session, transaction_ids[3], transactionBody.put("type", "withdrawal").put("amount", 200));
        // change withdrawal 89 to 100, balance - 11
        editTransaction(session, transaction_ids[5], transactionBody.put("type", "withdrawal").put("amount", 100));
        // change deposit 430 to 375, balance - 55
        editTransaction(session, transaction_ids[6], transactionBody.put("type", "deposit").put("amount", 375));
        // change deposit 175 to 220, balance + 45
        editTransaction(session, transaction_ids[2], transactionBody.put("type", "deposit").put("amount", 220));
        assertEquals(new Float(finalBalance + 99 - 11 - 55 + 45), getBalance(session));
    }

    @Test
    public void testDeleteTransactions() {
        String session = getValidSessionId();
        JSONObject transactionBody = new JSONObject()
                .put("externalIBAN", "NL39RABO0300065264");
        // 473-201+82-136+88-12+1468-1099+43-82+25 = 649
        Float finalBalance = 649f;
        List<Integer> amounts = Arrays.asList(473,201,82,136,88,12,1468,1099,43,82,25);
        int[] transaction_ids = new int[11];
        String type = "deposit";
        for (int i = 0; i < 11; i++) {
            transactionBody.put("amount", amounts.get(i));
            transactionBody.put("type", type);
            if (type.equals("deposit")) {
                type = "withdrawal";
            } else {
                type = "deposit";
            }
            transaction_ids[i] = createTransaction(session, transactionBody);
        }
        assertEquals(finalBalance, getBalance(session));
        // delete deposit 88, balance - 88
        deleteTransaction(session, transaction_ids[4]);
        // delete withdrawal 82, balance + 82
        deleteTransaction(session, transaction_ids[9]);
        assertEquals(new Float(finalBalance - 88 + 82), getBalance(session));
    }

    private int createTransaction(String session, JSONObject data) {
        data.put("date", Long.toString(System.currentTimeMillis()));
        return given()
                    .header(X_SESSION_ID_HEADER, session)
                    .contentType(APPLICATION_JSON_VALUE)
                    .body(data.toString())
                    .when()
                    .post(BASE_URL + TRANSACTIONS_PATH)
                    .then().assertThat()
                    .statusCode(201)
                    .extract().path("id");
    }

    private void editTransaction(String session, int id, JSONObject data) {
        data.put("date", Long.toString(System.currentTimeMillis()));
        given()
            .header(X_SESSION_ID_HEADER, session)
            .contentType(APPLICATION_JSON_VALUE)
            .body(data.toString())
            .when()
            .put(BASE_URL + TRANSACTIONS_PATH + "/" + id)
            .then().assertThat()
            .statusCode(200);
    }

    private void deleteTransaction(String session, int id) {
        given()
            .header(Utils.X_SESSION_ID_HEADER, session)
            .when()
            .delete(Utils.BASE_URL + Utils.TRANSACTIONS_PATH + "/" + id)
            .then()
            .assertThat()
            .statusCode(204);
    }

    private Float getBalance(String session) {
        return given()
                    .header(X_SESSION_ID_HEADER, session)
                    .when()
                    .get(BASE_URL + SESSIONS_PATH)
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .extract().path("balance");
    }
}
