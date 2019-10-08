package myProject;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLTextRenderer;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import transforms.*;


/**
 *
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2019-09-02
 */
public class Renderer extends AbstractRenderer{

    int width, height;

    // Axis and mouseButton switch
    double ox, oy;
    private boolean isButtonPressed = false;

    OGLBuffers buffers;

    // All shits for shaders
    int shaderProgram, locProjection, locView, locRotateX;

    // Rotation counter
    float rotate = 0;

    // Model, View and Projection matrix (KIKM-PGRF3/prednasky/PG3_01.pdf slide: 12)
    Mat4RotX rotateX = new Mat4RotX(rotate);
    Camera view = new Camera();
    Mat4 projection = new Mat4PerspRH(Math.PI / 4, 1, 0.01, 10000.0);

    private GLFWKeyCallback   keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if (action == GLFW_PRESS || action == GLFW_REPEAT){
                switch (key) {
                    case GLFW_KEY_W:
                        view = view.forward(1);
                        break;
                    case GLFW_KEY_D:
                        view = view.right(1);
                        break;
                    case GLFW_KEY_S:
                        view = view.backward(1);
                        break;
                    case GLFW_KEY_A:
                        view = view.left(1);
                        break;
                    case GLFW_KEY_LEFT_CONTROL:
                        view = view.down(1);
                        break;
                    case GLFW_KEY_LEFT_SHIFT:
                        view = view.up(1);
                        break;
                    case GLFW_KEY_SPACE:
                        view = view.withFirstPerson(!view.getFirstPerson());
                        break;
                    case GLFW_KEY_R:
                        view = view.mulRadius(0.9f);
                        break;
                    case GLFW_KEY_F:
                        view = view.mulRadius(1.1f);
                        break;
                }
            }
        }
    };

    private GLFWWindowSizeCallback wsCallback = new GLFWWindowSizeCallback() {
        @Override
        public void invoke(long window, int w, int h) {
            if (w > 0 && h > 0 &&
                    (w != width || h != height)) {
                width = w;
                height = h;
                projection = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
                if (textRenderer != null)
                    textRenderer.resize(width, height);
            }
        }
    };

    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback () {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            isButtonPressed = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;

            if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS){
                isButtonPressed = true;
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                ox = xBuffer.get(0);
                oy = yBuffer.get(0);
            }

            if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE){
                isButtonPressed = false;
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
    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (isButtonPressed) {
                view = view.addAzimuth((double) Math.PI * (ox - x) / width)
                        .addZenith((double) Math.PI * (oy - y) / width);
                ox = x;
                oy = y;
            }
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            if (dy < 0)
                view = view.mulRadius(0.9f);
            else
                view = view.mulRadius(1.1f);

        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWWindowSizeCallback getWsCallback() {
        return wsCallback;
    }

    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mbCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallbacknew;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }

    void createBuffers() {
        GridFactory factory = new GridFactory(10,10);
        float[] vertexBufferData = factory.getVertexBuffer();
        int[] indexBufferData = factory.getIndexBuffer();

        // vertex binding description, concise version
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2), // 2 floats
        };

        buffers = new OGLBuffers(vertexBufferData, attributes, indexBufferData);
    }

    @Override
    public void init() {
        OGLUtils.printOGLparameters();
        glClearColor(0f, 0f, 0f, 1.0f);

        createBuffers();

        shaderProgram = ShaderUtils.loadProgram("/myProject/myShader.vert",
                "/myProject/myShader.frag",
                null,null,null,null);

        // Shader program set
        glUseProgram(this.shaderProgram);

        // internal OpenGL ID of a shader uniform (constant during one draw call
        // - constant value for all processed vertices or pixels) variable
        locProjection = glGetUniformLocation(shaderProgram, "projection");
        locView = glGetUniformLocation(shaderProgram, "view");
        locRotateX = glGetUniformLocation(shaderProgram, "rotateX");

        view = view.withPosition(new Vec3D(5, 5, 2.5))
                .withAzimuth(Math.PI * 1.25)
                .withZenith(Math.PI * -0.125);

        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glEnable(GL_DEPTH_TEST);

        textRenderer = new OGLTextRenderer(width, height);
    }

    @Override
    public void display() {
        String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");

        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, width, height);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

        glUseProgram(shaderProgram);
        // to use the default shader of the "fixed pipeline", call
        //glUseProgram(0);
        rotate += 0.01;
        rotateX = new Mat4RotX(rotate);
        glUniformMatrix4fv(locProjection, false, projection.floatArray());
        glUniformMatrix4fv(locView, false, view.getViewMatrix().floatArray());
        glUniformMatrix4fv(locRotateX, false, rotateX.floatArray());

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        // bind and draw
        buffers.draw(GL_TRIANGLES, shaderProgram);

        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(width-90, height-3, " (c) PGRF UHK");
        textRenderer.draw();
    }
}