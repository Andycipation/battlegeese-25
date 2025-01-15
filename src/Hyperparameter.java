package src;
public final class Hyperparameter {
    // current value: 600    range: [0, 2000]
    public static final int lateGameCountThres = 600;

    // current value: 50    range: [0, 100]
    public static final int paintToMoneyEarly = 50;

    // current value: 75    range: [0, 100]
    public static final int paintToMoneyLate = 75;

    // current value: 15    range: [0, 25]
    public static final int timeRuin = 15;

    // current value: 5    range: [0, 25]
    public static final int timeTravel = 5;

    // current value: 1250    range: [1000, 1500]
    public static final int noSpawnThresEarly = 1250;

    // current value: 5000    range: [1000, 10000]
    public static final int noSpawnThresLate = 5000;

    // current value: 5000    range: [2000, 20000]
    public static final int lateGameChipThres = 5000;

    private Hyperparameter() {}
}
