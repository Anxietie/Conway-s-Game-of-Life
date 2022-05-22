public class Cell {
    final private int x;
    final private int y;
    private boolean alive;

    public Cell(int x, int y, boolean alive) {
        this.x = x;
        this.y = y;
        this.alive = alive;
    }

    public int x() { return x; }
    public int y() { return y; }
    public boolean alive() { return alive; }
    public void changeState() { alive = !alive; }

    // Loops through the immediate surrounding cells of the current cell and returns how many are alive
    public int getAliveNeighbors(Cell[][] cells, int cellLength) {
        int an = 0;
        int x = x()/cellLength;
        int y = y()/cellLength;
        for (int r = y-1; r <= y+1; r++) {
            for (int c = x-1; c <= x+1; c++) {
                if (c == x && r == y) continue;
                try {
                    if (cells[r][c].alive()) an++;
                }
                catch (IndexOutOfBoundsException ioobe) {
                    // Ignore
                }
            }
        }
        return an;
    }
}
