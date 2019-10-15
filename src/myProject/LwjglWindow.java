package myProject;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.IOException;
import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class LwjglWindow {

    public static int WIDTH = 600;
    public static int HEIGHT = 400;

    // The window handle
    private long window;
    private AbstractRenderer renderer;

    private static boolean DEBUG = false;

    static {
        if (DEBUG) {
            System.setProperty("org.lwjgl.util.Debug", "true");
            System.setProperty("org.lwjgl.util.NoChecks", "false");
            System.setProperty("org.lwjgl.util.DebugLoader", "true");
            System.setProperty("org.lwjgl.util.DebugAllocator", "true");
            System.setProperty("org.lwjgl.util.DebugStack", "true");
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
        }
    }

    public LwjglWindow(AbstractRenderer renderer) throws IOException {
        this(WIDTH, HEIGHT, renderer, false);
    }

    public LwjglWindow(AbstractRenderer renderer, boolean debug) throws IOException {
        this(WIDTH, HEIGHT, renderer, debug);
    }

    public LwjglWindow(int width, int height, AbstractRenderer renderer, boolean debug) throws IOException {
        this.renderer = renderer;
        DEBUG = debug;
        WIDTH = width;
        HEIGHT = height;
        run();
    }

    public void run() throws IOException {
        init();

        loop();

        renderer.dispose();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        String text = new String(renderer.getClass().getName() );
        text = text.substring(0,text.lastIndexOf('.'));
        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, text, NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, renderer.getKeyCallback());
        glfwSetWindowSizeCallback(window,renderer.getWsCallback());
        glfwSetMouseButtonCallback(window,renderer.getMouseCallback());
        glfwSetCursorPosCallback(window,renderer.getCursorCallback());
        glfwSetScrollCallback(window,renderer.getScrollCallback());

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
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

    private void loop() throws IOException {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        if (DEBUG)
            GLUtil.setupDebugMessageCallback();

        renderer.init();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {

            renderer.display();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

}