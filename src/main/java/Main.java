import com.authy.AuthyApiClient;
import com.authy.AuthyUtil;
import com.authy.api.Hash;
import com.authy.api.Token;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static String AUTHY_KEY = "";

    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/public");


        if (System.getenv("AUTHY_KEY") != null) {
            AUTHY_KEY = System.getenv("AUTHY_KEY");
        }


        get("/", (req, res) -> "Hello Authy! " + (AUTHY_KEY.length() == 0 ? "PLEASE SET YOUR AUTHY KEY USING: heroku config:set AUTHY_KEY=YOURKEYHERE as specified at: https://devcenter.heroku.com/articles/config-vars" : "Authy key is ready!"));


        get("/approved", (request, response) -> {

            try {

                // let's create a Map of Strings to put the query parameters
                HashMap<String, String> params = new HashMap<>();
                for (String p : request.queryParams()) {
                    params.put(p, request.queryParams(p));
                }

                // let's create a Map of Strings to put the query headers
                HashMap<String, String> headers = new HashMap<>();
                for (String h : request.headers()) {
                    headers.put(h, request.headers(h));
                }


                if (!AuthyUtil.validateSignatureForGet(params, headers, request.url(), AUTHY_KEY)) {
                    throw new SecurityException("Invalid Signature");
                }

                LOGGER.log(Level.INFO, "Signature is valid(GET).");

                response.type("application/json");
                return new JSONObject("{}").toString();
            } catch (Exception ex) {
                ex.printStackTrace();
                return ex.getMessage();
            }


        });

        post("/approved", (request, response) -> {

            try {


                // let's create a Map of Strings to put the query headers
                HashMap<String, String> headers = new HashMap<>();
                for (String h : request.headers()) {
                    headers.put(h, request.headers(h));
                }

                if (!AuthyUtil.validateSignatureForPost(request.body(), headers, request.url(), AUTHY_KEY)) {
                    throw new SecurityException("Invalid Signature");
                }

                LOGGER.log(Level.INFO, "Signature is valid(POST).");

                response.type("application/json");

                return new JSONObject("{}").toString();
            } catch (Exception ex) {
                ex.printStackTrace();
                return ex.getMessage();
            }


        });

        get("/ask", (request, response) -> {

            try {
                AuthyApiClient client = new AuthyApiClient(AUTHY_KEY);
                System.out.println(client.getUsers());
                System.out.println(request.queryParams("userId"));
                Hash tmp = client.getUsers().requestSms(Integer.parseInt(request.queryParams("userId")));
                return "SMS sent";
            } catch (Exception ex) {
                ex.printStackTrace();
                return "ERROR";
            }


        });

        get("/onetouch", (request, response) -> {

            try {
                AuthyApiClient client = new AuthyApiClient(AUTHY_KEY);
                Hash tmp = client.getUsers().requestSms(Integer.parseInt(request.queryParams("userId")));
                return "SMS sent";
            } catch (Exception ex) {
                ex.printStackTrace();
                return "ERROR";
            }

        });

        get("/validate", (request, response) -> {
            try {
                AuthyApiClient client = new AuthyApiClient(AUTHY_KEY);
                Token tk = client.getTokens().verify(Integer.parseInt(request.queryParams("userId")), request.queryParams("token"));
                return "Token:" + tk.toJSON();
            } catch (Exception ex) {
                ex.printStackTrace();
                return "ERROR";
            }


        });

    }

}
