public class Clock {
    private double time;
    private double rate;
    public Clock() {
        time = 0;
        rate = 0.10;
    }
    public void update() {
        if (time >= 1) time = 0;
        time += rate;
    }
    public void setRate(double r) {
        rate = r;
    }
    public double getTime() {
        return time;
    }
    public boolean tick() {
        return time >= 1.0;
    }
    public double getRate() {
        return rate;
    }
}
