public class Clock {
    private double time;
    private double rate;
    public Clock() {
        time = 0;
        rate = 10;
    }
    public void update() {
        if (time >= 100) time = 0;
        time += rate;
    }
    public void setRate(double r) {
        rate = r;
    }
    public int getTime() {
        return time;
    }
    public boolean tick() {
        return time >= 100;
    }
    public int getRate() {
        return rate;
    }
}
