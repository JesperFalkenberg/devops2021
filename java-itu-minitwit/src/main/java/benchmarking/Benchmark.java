package benchmarking;
// Simple microbenchmark setups
// sestoft@itu.dk * 2013-06-02, 2015-09-15

import java.util.List;
import java.util.Random;
import java.util.function.IntToDoubleFunction;

class Benchmark {
  public static void main(String[] args) {
    //setup
    final int USERS_TO_ADD     = 20_000;
    final int FOLLOWERS_TO_ADD = 40_000;
    final int MESSAGES_TO_ADD  = 40_000;
    boolean dbExists = false;
    final String[] usernames = CreateAndFillTestDB.genUsernames(USERS_TO_ADD);
    if (!dbExists) {
      CreateAndFillTestDB.instantiateDB();
      System.out.println("start adding users");
      CreateAndFillTestDB.addUsers(USERS_TO_ADD, usernames);
      System.out.println("end adding users");
      System.out.println("start adding followers");
      CreateAndFillTestDB.addFollowers(FOLLOWERS_TO_ADD, usernames);
      System.out.println("end adding followers");
      System.out.println("start adding messages");
      CreateAndFillTestDB.addMessages(MESSAGES_TO_ADD, USERS_TO_ADD);
      System.out.println("end adding messages");
    }

    System.out.println("start testing");


    //tests
    SystemInfo();
    final Random rand = new Random();
    Mark8("GetUserId",    i -> DBBenchmarkableFunctions.runGetUserId(   rand, USERS_TO_ADD, usernames));
    Mark8("GetUser",      i -> DBBenchmarkableFunctions.runGetUser(     rand, USERS_TO_ADD, usernames));
    Mark8("GetUserById",  i -> DBBenchmarkableFunctions.runGetUserById( rand, USERS_TO_ADD));
    Mark8("CountUsers",       i -> DBBenchmarkableFunctions.runCountUsers(     rand));
    Mark8("CountFollowers",   i -> DBBenchmarkableFunctions.runCountFollowers( rand));
    Mark8("CountMessages",    i -> DBBenchmarkableFunctions.runCountMessages(  rand));
    Mark8("publicTimeline",     i -> DBBenchmarkableFunctions.runPublicTimeline());
    Mark8("TweetsByUsername",   i -> DBBenchmarkableFunctions.runTweetsByUsername(   rand, USERS_TO_ADD, usernames));
    Mark8("PersonalTweetsById", i -> DBBenchmarkableFunctions.runPersonalTweetsById( rand, USERS_TO_ADD));
  }
  // ========== Infrastructure code ==========

  public static void SystemInfo() {
    System.out.printf("# OS:   %s; %s; %s%n",
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"));
    System.out.printf("# JVM:  %s; %s%n",
            System.getProperty("java.vendor"),
            System.getProperty("java.version"));
    // The processor identifier works only on MS Windows:
    System.out.printf("# CPU:  %s; %d \"cores\"%n",
            System.getenv("PROCESSOR_IDENTIFIER"),
            Runtime.getRuntime().availableProcessors());
    java.util.Date now = new java.util.Date();
    System.out.printf("# Date: %s%n",
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now));
  }

  public static double Mark8(String msg, String info, IntToDoubleFunction f, int n, double minTime) {
    int count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    double timeSpentPausingOnce = getTimeSpentPausingOnce();
    do {
      count *= 2;
      double timeSpentPausingCount = timeSpentPausingOnce*count;
      st = sst = 0.0;
      Timer totalTime = new Timer();
      totalTime.play();
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++) {
          t.play();
          dummy += f.applyAsDouble(i);
          t.pause();
        }
        double time = Math.max((t.check()-timeSpentPausingCount) * 1e9 / count, 0.0);
        totalCount += count;
        st += time;
        sst += time * time;
      }
      totalTime.pause();
      runningTime = totalTime.check() / n;
    } while (runningTime < minTime && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %s%15.1f ns %10.2f %10d%n", msg, info, mean, sdev, count);
    return dummy / totalCount;
  }
  public static double Mark8(String msg, IntToDoubleFunction f) {
    return Mark8(msg, "", f, 10, 0.25);
  }
  public static double getTimeSpentPausingOnce(){
    Timer tForTimePausePlay = new Timer();
    long timesPaused = 10_000_000;
    for (int paused = 0; paused < timesPaused; paused++) { tForTimePausePlay.play(); tForTimePausePlay.pause(); }
    return 0.9*tForTimePausePlay.check() / timesPaused;    //0.9 to counter garbadge collection as that part rarely takes time normally
  }
  public static double Mark8Setup(String msg, String info, Benchmarkable f, int n, double minTime) {
    int count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    double timeSpentPausingOnce = getTimeSpentPausingOnce();
    do {
      count *= 2;
      double timeSpentPausingCount = timeSpentPausingOnce*count;
      st = sst = 0.0;
      Timer totalTime = new Timer();
      totalTime.play();
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++) {
          f.setup();
          t.play();
          dummy += f.applyAsDouble(i);
          t.pause();
        }
        double time = Math.max((t.check()-timeSpentPausingCount) * 1e9 / count, 0.0);
        totalCount += count;
        st += time;
        sst += time * time;
      }
      totalTime.pause();
      runningTime = totalTime.check();
    } while (runningTime < minTime && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %s%15.1f ns %10.2f %10d%n", msg, info, mean, sdev, count);
    return dummy / totalCount;
  }
  public static double Mark8Setup(String msg, String info, Benchmarkable f) { return Mark8Setup(msg, info, f, 10, 0.25); }
}