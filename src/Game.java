import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Game {
    private long window;
    private int width;
    private int height;
    private Cell[][] cells;
    private int cellLength;
    private boolean running;
    private Clock timer;

    // The entire game pretty much
    public void run() {
        System.out.println("Successfully begun with LWJGL " + Version.getVersion());

        // Initialization, the render loop, and then cleaning up
        init();
        try {
            loop();
        }
        catch (InterruptedException ie) {
            // Ignore
        }
        end();
    }

    public void init() {
        // Set up an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        // Support for a resizable window is possible if the cells array is resized accordingly
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE); // the window will not be resizable

        // Window dimensions
        width = 800;
        height = 800;

        // Set the running flag which will be toggled with space bar using the key callback
        running = false;

        // Sets up a clock that the iterate method will sync to
        timer = new Clock();

        // Create the window
        window = glfwCreateWindow(width, height, "Conway's Game of Life", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Set up a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (key == GLFW_KEY_SPACE && action == GLFW_RELEASE)
                running = !running;
            if (key == GLFW_KEY_C && action == GLFW_RELEASE)
                killCells();
            if (key == GLFW_KEY_UP && action == GLFW_RELEASE)
                if (timer.getRate() < 0.30) timer.setRate(timer.getRate()+0.05);
            if (key == GLFW_KEY_DOWN && action == GLFW_RELEASE)
                if (timer.getRate() > 0.10) timer.setRate(timer.getRate()-0.05);
        });

        // Set up a mouse callback. It will be called every time the mouse is clicked, repeated, or released.
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == 0 && action == GLFW_RELEASE) {
                DoubleBuffer tx = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer ty = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, tx, ty);

                int x = (int)tx.get(0);
                int y = (int)ty.get(0);
                tx.clear();
                ty.clear();

                x /= cellLength;
                y = (height - y)/cellLength;

                try {
                    cells[y][x].changeState();
                }
                catch (IndexOutOfBoundsException ioobe) {
                    // Ignore
                }
            }
        });

        // Initialize the grid of cells
        initCells();

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    public void initCells() {
        cellLength = 10;
        cells = new Cell[width/cellLength][height/cellLength];

        // Random seed
        for (int r = 0; r < cells.length; r++) {
            for (int c = 0; c < cells[r].length; c++) {
                boolean alive = ((int)(Math.random()*2)) == 1;
                cells[r][c] = new Cell(c*cellLength, r*cellLength, alive);
            }
        }
    }

    public void iterate() {
        // Creates a temporary Cells[][] to hold all the information for the next generation
        // as to not change cells while accessing it
        Cell[][] iterated = new Cell[height/cellLength][width/cellLength];
        for (int r = 0; r < cells.length; r++) {
            for (int c = 0; c < cells[0].length; c++) {
                // If the cell is alive and has 2 or 3 alive neighbors; it stays alive. Otherwise, it dies.
                // If the cell is dead and has 3 alive neighbors; it comes back to life. Otherwise, it stays dead.
                int aliveNeighbors = cells[r][c].getAliveNeighbors(cells, cellLength);
                if (cells[r][c].alive() && (aliveNeighbors != 2 && aliveNeighbors != 3))
                    iterated[r][c] = new Cell(c*cellLength, r*cellLength, false);
                else if (aliveNeighbors == 3)
                    iterated[r][c] = new Cell(c*cellLength, r*cellLength, true);
                else
                    iterated[r][c] = cells[r][c];
            }
        }
        cells = iterated;
    }

    public void drawCell(double x, double y) {
        // Draws a quad at the coordinate
        glColor3d(1.0, 1.0, 1.0);
        glBegin(GL_QUADS);

        glVertex2d(x, y);                                   // bottom left
        glVertex2d(x + cellLength, y);                   // bottom right
        glVertex2d(x + cellLength, y + cellLength);   // top right
        glVertex2d(x, y + cellLength);                   // top left

        glEnd();
    }

    public void drawCells() {
        for (Cell[] row : cells) {
            for (Cell c : row) {
                if (c.alive())
                    drawCell(c.x(), c.y());
            }
        }
    }

    // Resets the grid of cells
    public void killCells() {
        for (Cell[] row : cells) {
            for (Cell c : row)
                if (c.alive()) c.changeState();
        }
    }

    public void loop() throws InterruptedException {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Set the projection
        glMatrixMode(GL_PROJECTION);
        glOrtho(0.0, width, 0.0, height, -1.0, 1.0);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Draw the cells
            drawCells();
            // Get next generation
            if (running && timer.tick())
                iterate();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();

            // Updates timer
            timer.update();
        }
    }

    public void end() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
