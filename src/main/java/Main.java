import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

import com.authy.AuthyApiClient;
import com.authy.AuthyUtil;
import com.authy.api.Hash;
import com.authy.api.Token;
import org.json.JSONObject;
import spark.ModelAndView;

import static spark.Spark.get;

import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main {

    private static final String TOKEN = "YOUR AUTHY TOKEN";

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());


    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/public");

        get("/hello", (req, res) -> "Hello World");

        get("/", (req, res) -> "Hello Index!");


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


                if (!AuthyUtil.validateSignatureForGet(params, headers, request.url(), TOKEN)) {
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

                if (!AuthyUtil.validateSignatureForPost(request.body(), headers, request.url(), TOKEN)) {
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
                AuthyApiClient client = new AuthyApiClient(TOKEN);
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
                AuthyApiClient client = new AuthyApiClient(TOKEN);
                Hash tmp = client.getUsers().requestSms(Integer.parseInt(request.queryParams("userId")));
                return "SMS sent";
            } catch (Exception ex) {
                ex.printStackTrace();
                return "ERROR";
            }


        });

        get("/validate", (request, response) -> {
            try {
                AuthyApiClient client = new AuthyApiClient(TOKEN);

                Token tk = client.getTokens().verify(Integer.parseInt(request.queryParams("userId")), request.queryParams("token"));
                System.out.println("TOKEN: " + tk);
                return "Token:" + tk.toJSON();
            } catch (Exception ex) {
                ex.printStackTrace();
                return "ERROR";
            }


        });

    }

}
