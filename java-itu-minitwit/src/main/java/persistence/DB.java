package persistence;

import model.Follower;
import model.Message;
import model.User;
import errorhandling.Failure;
import errorhandling.Result;
import errorhandling.Success;
import com.dieselpoint.norm.Database;

public class DB {
    private static Database instance;
    static final int PORT                  = 3306;
    private static String database         = "minitwit";
    private static final String IP         = "localhost";
    private static String user             = "root";
    private static String pw               = "root";
    private static String connectionString = null;

    private DB() {}

    private static void setSystemProperties() {
        if(connectionString != null) {
            System.setProperty("norm.jdbcUrl", "jdbc:" + connectionString);
        } else {
            System.setProperty("norm.jdbcUrl", "jdbc:mysql://" + IP + ":" + PORT + "/" + database + "?allowPublicKeyRetrieval=true&useSSL=false");
        }
        System.setProperty("norm.user", user);
        System.setProperty("norm.password", pw);
    }

    /*
        Returns a new connection to the database.
    */
    public static Result<Database> connectDb() {
        if (instance == null) {
            try {
                setSystemProperties();

                instance = new Database();
            } catch (Exception e) {
                return new Failure<>(e);
            }
        }

        return new Success<>(instance);
    }

    public static void setCONNECTIONSTRING(String connectionString) {
        DB.connectionString = connectionString;
    }

    public static void setUSER(String user) {
        DB.user = user;
    }

    public static void setPW(String pw) {
        DB.pw = pw;
    }

    public static void setDATABASE(String dbName) {
        database = dbName;
    }

    public static Database initDb()  {
        return DB.connectDb().get();
    }
    public static void dropDB(){
        var db = initDb();
        db.sql("drop table if exists follower").execute();
        db.sql("drop table if exists message").execute();
        db.sql("drop table if exists user").execute();

        db.createTable(User.class);
        db.createTable(Message.class);
        db.sql("ALTER TABLE message ADD FOREIGN KEY (authorId) REFERENCES user(id)").execute();
        db.createTable(Follower.class);
        db.sql("ALTER TABLE follower ADD FOREIGN KEY (whoId) REFERENCES user(id)").execute();
        db.sql("ALTER TABLE follower ADD FOREIGN KEY (whomId) REFERENCES user(id)").execute();
    }

    public static String getDatabase() {
        return database;
    }

    public static String getIP() {
        return IP;
    }

    public static String getUser() {
        return user;
    }

    public static String getPw() {
        return pw;
    }

    public static String getConnectionString() {
        return connectionString;
    }

    public static void removeInstance() {
        System.clearProperty("norm.jdbcUrl");
        System.clearProperty("norm.user");
        System.clearProperty("norm.password");
        if (instance == null) {
            return;
        }
        instance.close();
        instance = null;
    }
}