
import persistence.DB;
import persistence.MessageRepository;
import persistence.UserRepository;
import errorhandling.Result;
import errorhandling.Success;
import org.junit.jupiter.api.BeforeEach;

import static spark.Spark.stop;

abstract class DatabaseTestBase {
    int userId = 1;

    @BeforeEach
    void setUp() {
        DB.setDATABASE("testMinitwit");
        if (System.getProperty("DB_TEST_CONNECTION_STRING") != null) {
            DB.setCONNECTIONSTRING(System.getProperty("DB_TEST_CONNECTION_STRING"));
            DB.setUSER(System.getProperty("DB_USER"));
            DB.setPW(System.getProperty("DB_PASSWORD"));
        }
        DB.dropDB();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        stop();
    }

    Result<String> register(String username, String password, String email){
        if (email==null)     email = username + "@example.com";
        return UserRepository.addUser(username, email, password);
    }

    Result<Boolean> login(String username, String password) {
        return UserRepository.queryLogin(username, password);
    }

    Result<Boolean> registerAndLogin(String username, String password) {
        this.register(username, password, null);
        return login(username, password);
    }

    Result<Integer> registerLoginGetID(String username, String password, String email) {
        this.register(username, password, email);
        this.login(username, password);
        var id = UserRepository.getUserId(username);
        assert (id.isSuccess());
        assert (id.get() == userId);
        userId++;
        return id;
    }

    Result<Boolean> logout() {
        //todo logout helper function
        return new Success<>(true);
    }

    void addMessage(String text, int loggedInUserId) {
        var rs = MessageRepository.addMessage(text, loggedInUserId);
        assert (rs.get());
        try {
            //hotfix: added to ensure order of messages
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}