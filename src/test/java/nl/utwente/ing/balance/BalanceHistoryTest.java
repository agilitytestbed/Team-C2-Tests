package nl.utwente.ing.balance;

import io.restassured.path.json.JsonPath;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static nl.utwente.ing.Utils.*;
import static org.junit.Assert.assertEquals;

public class BalanceHistoryTest {

    @Test
    public void testTimeIntervals() {
        String session_id = getValidSessionId();
        String[] intervals = new String[]{"hour", "day", "week", "month", "year"};
        for (String interval : intervals) {
            JsonPath jsonHistory = given()
                                .header(X_SESSION_ID_HEADER, session_id)
                                .param("interval", interval)
                                .when()
                                .get(BASE_URL + BALANCE_PATH + "/history")
                                .then()
                                .assertThat()
                                .statusCode(200)
                                .extract().jsonPath();
            ArrayList<HashMap<String, Object>> history = jsonHistory.get("$");
            DateTime timestamp = new DateTime(history.get(0).get("timestamp"));
            for (int i = 1; i < history.size(); i++) {
                DateTime newTimestamp = new DateTime(history.get(i).get("timestamp"));
                switch (interval) {
                    case "hour":  assertEquals(timestamp.minusHours(1), newTimestamp);
                                  break;
                    case "day":   assertEquals(timestamp.minusDays(1), newTimestamp);
                                  break;
                    case "week":  assertEquals(timestamp.minusWeeks(1), newTimestamp);
                                  break;
                    case "month": assertEquals(timestamp.minusMonths(1), newTimestamp);
                                  break;
                    case "year":  assertEquals(timestamp.minusYears(1), newTimestamp);
                    default: break;
                }
                timestamp = newTimestamp;
            }
        }
    }

    @Test
    public void testDefaultBehaviour() {
        String session_id = getValidSessionId();
        JsonPath jsonHistory = given()
                .header(X_SESSION_ID_HEADER, session_id)
                .when()
                .get(BASE_URL + BALANCE_PATH + "/history")
                .then()
                .assertThat()
                .statusCode(200)
                .extract().jsonPath();
        ArrayList<HashMap<String, Object>> history = jsonHistory.get("$");
        assertEquals(24, history.size());
        DateTime firstTimestamp = new DateTime(history.get(0).get("timestamp"));
        DateTime secondTimestamp = new DateTime(history.get(1).get("timestamp"));
        assertEquals(firstTimestamp.minusMonths(1), secondTimestamp);
    }

    @Test
    public void testExample() {
        String session_id = getValidSessionId();
        System.out.println(session_id);
        DateTime currentDate = new DateTime();
        String[] types = new String[]{"deposit", "withdrawal"};
        for (int i = 0; i < 24; i++) {
            DateTime newDateTime = currentDate;
            for (int j = 0; j < 4; j++) {
                newDateTime = newDateTime.minusHours(1);
                int amount = new Random().nextInt(100);
                String type = types[new Random().nextInt(types.length)];
                JSONObject transactionBody = new JSONObject()
                        .put("date", newDateTime.toDate().getTime())
                        .put("externalIBAN", "NL39RABO0300065264")
                        .put("amount", amount)
                        .put("type", type);
                given()
                    .header(X_SESSION_ID_HEADER, session_id)
                    .contentType(APPLICATION_JSON_VALUE)
                    .body(transactionBody.toString())
                    .when()
                    .post(BASE_URL + TRANSACTIONS_PATH)
                    .then().assertThat()
                    .statusCode(201);
            }
            currentDate = currentDate.minusMonths(1);
        }
    }
}
