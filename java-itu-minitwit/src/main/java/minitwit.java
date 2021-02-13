import RoP.Failure;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import spark.Request;
import spark.Response;

import java.util.HashMap;

import static spark.Spark.*;

public class minitwit {

    //configuration
    static Boolean DEBUG        = true;

    public static void main(String[] args) {
        try {
            staticFiles.location("/");

            before((request, response) -> Queries.before_request(request));

            after((request, response) ->  Queries.after_request());

            notFound((req, res) -> {
                res.type("application/json");
                return "{\"message\":\"Custom 404\"}";
            });

            internalServerError((req, res) -> {
                res.type("application/json");
                return "{\"message\":\"Custom 500 handling\"}";
            });

            registerEndpoints();

            //Queries.init_db();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void registerEndpoints() {

        get("/",                    (req, res)-> timeline(null, -1));
        get("/test",                (req, res)-> render_template("register.html"));
        get("/public",              (req, res)-> Queries.public_timeline());
        post("/add_message",        (req, res)-> add_message(null));
        post("/login",              (req, res)-> login("POST", req.params("username"), null, null));
        get("/login",               (req, res)-> login("GET", req.params("username"), null, null));
        get("/register",            (req, res)-> {
            System.out.println("register get");
            return render_template("register.html");
        });

        post("/register",           (req, res)-> register("POST", req.queryParams("username"), req.queryParams("email"), req.queryParams("password"), req.queryParams("password2")));
        get("/logout",              (req, res)-> logout());
        get("/:username",           (req, res)-> {
            System.out.println("username: get");
            return user_timeline(req.params("username"));
        });
        get("/:username/follow",    (req, res)-> follow_user(null));
        get("/:username/unfollow",  (req, res)-> unfollow_user(null));
    }

    private static Object render_template(String template, HashMap<String, Object> context) {
        try {
            Jinjava jinjava = new Jinjava();
            return jinjava.render(Resources.toString(Resources.getResource(template), Charsets.UTF_8), context);
        } catch (Exception e) {
            e.printStackTrace();
            return new Failure<>(e);
        }
    }

    private static Object render_template(String template) {
        return render_template(template, new HashMap<>());
    }


    /*
    Format a timestamp for display.
     */
    Result<String> format_datetime(String timestamp) {
        try {
            //https://stackoverflow.com/questions/18915075/java-convert-string-to-timestamp
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            Date date = formatter.parse(timestamp);
            return new Success<>(date.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new Failure<>(e);
        }
    }

    /*
    Return the gravatar image for the given email address.
     */
    String gravatar_url(String email, int size) {
        String encodedEmail = new String(email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        String hashHex = Hashing.generate_hash_hex(encodedEmail);
        return String.format("http://www.gravatar.com/avatar/%s?d=identicon&s=%d", hashHex, size);
    }

    /*
    Java does not support default arguments
     */
    String gravatar_url(String email) {
        return gravatar_url(email, 80);
    }

    /*
    Shows a users timeline or if no user is logged in it will
    redirect to the public timeline.  This timeline shows the user's
    messages as well as all the messages of followed users.
     */
    static Object timeline(String remote_addr, int user_id) {
        System.out.println("We got a visitor from: " + remote_addr);
        if (!Queries.user_logged_in()) {
            return public_timeline();
        }


        var rs = Queries.timeline(String.valueOf(user_id));
        return render_template("timeline.html");
    }
    /*
     Displays the latest messages of all users.
    */
    public static Object public_timeline() {
        var rs = Queries.public_timeline();
        return null;
        //return render_template("templates/timeline.html");
    }


    /*
    Display's a users tweets.
     */
    static Object user_timeline(Request request,String profile_username) {

        if (!Queries.user_logged_in(request)) {

            var profile_user = Queries.getUser(profile_username);

            return render_template("timeline.html", new HashMap<>() {{
                put("endpoint", "user_timeline");
                put("username", profile_username);
                put("title", profile_user.get().username() + "'s Timeline");
                put("profile_user_id", profile_user.get().user_id());
                put("profile_user_username", profile_user.get().username());
                put("messages", Queries.getTweetsByUsername(profile_username).get());
            }});
        } else {

            var user_id = getSessionUserId(request);

            var profile_user = Queries.getUser(profile_username);
            var logged_in_user = Queries.getUserById(user_id);

        return render_template("timeline.html", new HashMap<>() {{
            put("endpoint", "user_timeline");
            put("username", username);
            put("title", "My Timeline");
            put("user_id", user_id);
            put("profile_user_id", "0");
            put("followed", followed);
        }});
    }

    /*
    Adds the current user as follower of the given user.
     */
    static Object follow_user(String username) {

        var rs = Queries.follow_user(username);
        if (rs != null) {//flash('You are now following "%s"' % username)
        }
        else {return "404";}
        return null;
    }

    private static Integer getSessionUserId(Request request) {
        return request.session().attribute("user_id");
    }

    /*
    Removes the current user as follower of the given user.
     */
    static Object unfollow_user(String username) {
        var rs = Queries.follow_user(username);
        if (rs != null) {//flash('You are no longer following "%s"' % username)
        }
        else {return "404";}
        //return redirect(url_for('user_timeline', username=username))
        return null;
    }

    /*
    Registers a new message for the user.
     */
    static Object add_message(String message) {

        var rs = Queries.add_message(message);
        if (rs != null){
            //flash('You are no longer following "%s"' % username)
            //return redirect(url_for('user_timeline', username=username))
            //flash('Your message was recorded')
        }
        return timeline(null, -1);
    }

    /*
    Logs the user in.
     */
    static Object login(String HTTPVerb, String username, String password1, String password2) {
        if (Queries.user_logged_in()) {
            return render_template("timeline.html");
        }

        var loginResult = Queries.queryLogin(username, password);

        if (loginResult.isSuccess()) {
            request.session().attribute("user_id", Queries.getUserId(username).get());
            //TODO redirect to /
            return render_template("timeline.html", new HashMap<>() {{
                put("username", username);
                put("user", username);
                put("endpoint","My Timeline");
                put("title","My Timeline");
            }});
        } else {
            System.out.println(loginResult);

            return render_template("login.html", new HashMap<>() {{
                put("error", loginResult);
            }});
        }

    }

    /*
    Registers the user.
     */
    static Object register(String HTTPVerb, String username, String email, String password1, String password2) {
        if (Queries.user_logged_in()) {
            return render_template("timeline.html");
        }

        String error = Queries.register(HTTPVerb, username, email, password1, password2);
        if(error.length() > 0) System.out.println(error);

        String finalError = error; //must be effectively final
        return render_template("register.html", new HashMap<>() {{put("error", finalError); }});
    }

    /*
    Logs the user out
     */
    static Object logout(Request request) {
        System.out.println("You were logged out");
        request.session().removeAttribute("user_id");
        return render_template("timeline.html");
    }

}
