import com.authy.api.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.*;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static String AUTHY_KEY = "";

    private static String lastRequest = "{}";

    public static void main(String[] args) {

        port(Integer.valueOf(System.getenv("PORT")));
        staticFileLocation("/public");


        if (System.getenv("AUTHY_KEY") != null) {
            AUTHY_KEY = System.getenv("AUTHY_KEY");
        }


        get("/", (req, res) -> "<html><title>Hello Authy!</title><body> " + (AUTHY_KEY.length() == 0 ? "<b>PLEASE SET YOUR AUTHY KEY USING:</b> heroku config:set AUTHY_KEY=<i>YOURKEYHERE</i> <br>as specified at: <a href='https://devcenter.heroku.com/articles/config-vars'>here</a>" : "Authy key is ready!"));


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

                String uri = (request.raw().getHeader("X-Forwarded-Proto") != null ? request.raw().getHeader("X-Forwarded-Proto") : request.raw().getScheme()) + "://" +
                        request.raw().getServerName() +
                        ("http".equals(request.raw().getScheme()) && request.raw().getServerPort() == 80 || "https".equals(request.raw().getScheme()) && request.raw().getServerPort() == 443 ? "" : ":" + request.raw().getServerPort()) +
                        request.raw().getRequestURI();


                if (!AuthyUtil.validateSignatureForGet(params, headers, uri, AUTHY_KEY)) {
                    throw new SecurityException("Invalid Signature");
                }

                // lets just put this is a string to check it later.
                lastRequest = params.toString();

                LOGGER.log(Level.INFO, "Signature is valid(GET).");

                response.type("application/json");
                return new JSONObject("{}").toString();
            } catch (Exception ex) {
                ex.printStackTrace();
                return ex.getMessage();
            }


        });


        get("last-body", ((request, response) -> {
            response.type("text/plain");
            return lastRequest;
        }));

        post("/approved", (request, response) -> {

            try {


                // let's create a Map of Strings to put the query headers
                HashMap<String, String> headers = new HashMap<>();
                for (String h : request.headers()) {
                    headers.put(h, request.headers(h));
                }
                // let's just save the last body from Authy to display it later
                lastRequest = request.body();


                JSONObject obj = new JSONObject(lastRequest);


                String uri = (request.raw().getHeader("X-Forwarded-Proto") != null ? request.raw().getHeader("X-Forwarded-Proto") : request.raw().getScheme()) + "://" +
                        request.raw().getServerName() +
                        ("http".equals(request.raw().getScheme()) && request.raw().getServerPort() == 80 || "https".equals(request.raw().getScheme()) && request.raw().getServerPort() == 443 ? "" : ":" + request.raw().getServerPort()) +
                        request.raw().getRequestURI() +
                        (request.raw().getQueryString() != null ? "?" + request.raw().getQueryString() : "");

                if (!AuthyUtil.validateSignatureForPost(request.body(), headers, uri, AUTHY_KEY)) {
                    throw new SecurityException("Invalid Signature");
                }

                LOGGER.log(Level.INFO, "Signature is valid(POST).");

                response.type("application/json");


                return new JSONObject().put("status", obj.getString("status")).toString();
            } catch (Exception ex) {
                ex.printStackTrace();
                return ex.getMessage();
            }


        });

        get("/ask", (request, response) -> {

            try {
                AuthyApiClient client = new AuthyApiClient(AUTHY_KEY);
                Hash tmp = client.getUsers().requestSms(Integer.parseInt(request.queryParams("userId")));
                return tmp.toJSON();
            } catch (Exception ex) {
                ex.printStackTrace();
                return "ERROR";
            }


        });

        get("/onetouch", (request, response) -> {

            try {
                Integer userId = Integer.parseInt(request.queryParams("userId"));
                OneTouch oneTouch = new OneTouch("https://api.authy.com/", AUTHY_KEY);


                HashMap<String, String> details = new HashMap<String, String>();
                details.put("username", "User");
                details.put("location", "California,USA");

                HashMap<String, String> hidden = new HashMap<String, String>();
                hidden.put("ip_address", "10.10.3.203");

                List<Logo> logos = new ArrayList<>();
                try {
                    logos.add(new Logo(Logo.Resolution.Default, "https://s3.amazonaws.com/com.twilio.prod.cms-assets/Twilio_Press_Authy&Twilio.jpg"));
                    logos.add(new Logo(Logo.Resolution.Low, "https://s3.amazonaws.com/com.twilio.prod.cms-assets/Twilio_Press_Authy&Twilio.jpg"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                HashMap<String, Object> options = new HashMap<>();
                options.put("details", details);
                options.put("hidden_details", hidden);
                options.put("logos", logos);
                OneTouchResponse res = oneTouch.sendApprovalRequest(userId, "Please authorize me!", options, 100000);
                //Hash tmp = client.getUsers().requestSms());


                if (res.isSuccess()) {
                    return new JSONObject().put("succes", true).put("message", "LUCHO ROCKS").toString();
                } else {
                    return new JSONObject().put("succes", false).put("message", res.getMessage()).toString();
                }

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
