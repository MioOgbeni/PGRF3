package myProject;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import transforms.*;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * GLSL sample:<br/>
 * Read and compile shader from files "/shader/glsl01/start.*" using ShaderUtils
 * class in oglutils package
 * Manage (create, bind, draw) vertex and index buffers using OGLBuffers class
 * in oglutils package<br/>
 * Requires LWJGL3
 *
 * @author PGRF FIM UHK
 * @version 3.0
 * @since 2019-07-11
 */

public class HelloWorld {

	int width, height;

	double ox, oy;
	private boolean mouseButton = false;

	// The window handle
	private long window;

	OGLBuffers buffers;

	int shaderProgram, locProjection, locView, locRotateX;

	float time = 0;

	Mat4RotX rotateX = new Mat4RotX(time);
	Camera view = new Camera();
	Mat4 projection = new Mat4PerspRH(Math.PI / 4, 1, 0.01, 10000.0);

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

		// Create the window
		window = glfwCreateWindow(600, 600, "Hello World!", NULL, NULL);
		if ( window == NULL )
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			if (action == GLFW_PRESS || action == GLFW_REPEAT){
				switch (key){
					case GLFW_KEY_W:
						view = view.forward(1);
						break;
					case GLFW_KEY_S:
						view = view.backward(1);
						break;
					case GLFW_KEY_A:
						view = view.left(1);
						break;
					case GLFW_KEY_D:
						view = view.right(1);
						break;
					case GLFW_KEY_F:
						view = view.down(1);
						break;
					case GLFW_KEY_R:
						view = view.up(1);
						break;
				}
			}
		});

		glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double x, double y) {
				if (mouseButton) {
					view = view.addAzimuth((double) Math.PI * (ox - x) / width)
							.addZenith((double) Math.PI * (oy - y) / width);
					ox = x;
					oy = y;
				}
			}
		});

		glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {

			@Override
			public void invoke(long window, int button, int action, int mods) {
				mouseButton = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;

				if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS){
					mouseButton = true;
					DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
					DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
					glfwGetCursorPos(window, xBuffer, yBuffer);
					ox = xBuffer.get(0);
					oy = yBuffer.get(0);
				}

				if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE){
					mouseButton = false;
					DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
					DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
					glfwGetCursorPos(window, xBuffer, yBuffer);
					double x = xBuffer.get(0);
					double y = yBuffer.get(0);
					view = view.addAzimuth((double) Math.PI * (ox - x) / width)
							.addZenith((double) Math.PI * (oy - y) / width);
					ox = x;
					oy = y;
				}
			}

		});

		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 &&
                		(HelloWorld.this.width != width || HelloWorld.this.height != height)) {
                	HelloWorld.this.width = width;
                	HelloWorld.this.height = height;
                }
            }
        });

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

		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		OGLUtils.printOGLparameters();
		OGLUtils.printLWJLparameters();
		OGLUtils.printJAVAparameters();

		// Set the clear color
		glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

		createBuffers();

		shaderProgram = ShaderUtils.loadProgram("/myProject/myShader.vert",
				"/lvl1basic/p01start/uniform.frag",
				null,null,null,null);

		// Shader program set
		glUseProgram(this.shaderProgram);

		// internal OpenGL ID of a shader uniform (constant during one draw call
		// - constant value for all processed vertices or pixels) variable
		//locTime = glGetUniformLocation(shaderProgram, "time");
		locProjection = glGetUniformLocation(shaderProgram, "projection");
		locView = glGetUniformLocation(shaderProgram, "view");
		locRotateX = glGetUniformLocation(shaderProgram, "rotateX");

		view = view.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);
	}

	void createBuffers() {
		GridFactory factory = new GridFactory(10,10);
		float[] vertexBufferData = factory.getVertexBuffer();
        int[] indexBufferData = factory.getIndexBuffer();

		// vertex binding description, concise version
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2), // 2 floats
		};

		buffers = new OGLBuffers(vertexBufferData, attributes,
				indexBufferData);
		// the concise version requires attributes to be in this order within
		// vertex and to be exactly all floats within vertex

/*		full version for the case that some floats of the vertex are to be ignored
 * 		(in this case it is equivalent to the concise version):
 		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2, 0), // 2 floats, at 0 floats from vertex start
				new OGLBuffers.Attrib("inColor", 3, 2) }; // 3 floats, at 2 floats from vertex start
		buffers = new OGLBuffers(gl, vertexBufferData, 5, // 5 floats altogether in a vertex
				attributes, indexBufferData);
*/
	}

	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {

			glViewport(0, 0, width, height);

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			// init) in this sample (only one shader used)
			glUseProgram(shaderProgram);
			// to use the default shader of the "fixed pipeline", call
			//glUseProgram(0);
			time += 0.01;
			rotateX = new Mat4RotX(time);
			glUniformMatrix4fv(locProjection, false, projection.floatArray());
			glUniformMatrix4fv(locView, false, view.getViewMatrix().floatArray());
			glUniformMatrix4fv(locRotateX, false, rotateX.floatArray());

			glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
			// bind and draw
			buffers.draw(GL_TRIANGLES, shaderProgram);

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	public void run() {
		try {
			System.out.println("Hello LWJGL " + Version.getVersion() + "!");
			init();

			loop();

			// Free the window callbacks and destroy the window
			glfwFreeCallbacks(window);
			glfwDestroyWindow(window);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			// Terminate GLFW and free the error callback
			glDeleteProgram(shaderProgram);
			glfwTerminate();
			glfwSetErrorCallback(null).free();
		}

	}

	public static void main(String[] args) {
		new HelloWorld().run();
	}

}