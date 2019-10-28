package myProject;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.io.IOException;
import java.nio.DoubleBuffer;

import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

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
    int shaderProgram, locProjection, locView, locRotateX, locLightPos, locEyePos, paramFunc, renderTexture, lightType, mappingType;

    // Rotation counter
    float rotateValue = 0;


    float functionChanger = 0;
    float textureToggle = 0;
    float lightToggle = 0;
    float mappingToggle = 0;

    Boolean fill = true;
    Boolean rotate = true;
    // Model, View and Projection matrix (KIKM-PGRF3/prednasky/PG3_01.pdf slide: 12)
    Mat4RotZ rotateX = new Mat4RotZ(rotateValue);
    Camera view = new Camera();
    Mat4 projPers = new Mat4PerspRH(Math.PI / 4, 1, 0.01, 1000.0);
    Mat4 projOrth = new Mat4OrthoRH( 3, 3, 0.01, 1000.0);
    Boolean persp = true;
    Vec3D lightPos = new Vec3D(10, 0.5, 20);

    OGLTexture2D texture;
    OGLTexture2D texture_n;
    OGLTexture2D texture_h;
    OGLTexture2D.Viewer textureViewer;

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
                    case GLFW_KEY_1:
                        functionChanger = 0;
                        break;
                    case GLFW_KEY_2:
                        functionChanger = 1;
                        break;
                    case GLFW_KEY_3:
                        functionChanger = 2;
                        break;
                    case GLFW_KEY_4:
                        functionChanger = 3;
                        break;
                    case GLFW_KEY_5:
                        functionChanger = 4;
                        break;
                    case GLFW_KEY_F:
                        if(fill){
                            fill = false;
                        }else{
                            fill = true;
                        }
                        break;
                    case GLFW_KEY_R:
                        if(rotate){
                            rotate = false;
                        }else{
                            rotate = true;
                        }
                        break;
                    case GLFW_KEY_P:
                        if(persp){
                            persp = false;
                        }else{
                            persp = true;
                        }
                        break;
                    case GLFW_KEY_T:
                        if(textureToggle == 0){
                            textureToggle = 1;
                        }else{
                            textureToggle = 0;
                        }
                        break;
                    case GLFW_KEY_L:
                        if(lightToggle == 0){
                            lightToggle = 1;
                        }else{
                            lightToggle = 0;
                        }
                        break;
                    case GLFW_KEY_M:
                        if(mappingToggle == 0){
                            mappingToggle = 1;
                        }else if (mappingToggle == 1){
                            mappingToggle = 2;
                        }else{
                            mappingToggle = 0;
                        }
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
                if(persp){
                    projPers = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
                }else{
                    projOrth = new Mat4OrthoRH( height / (double) width, height / (double) width, 0.01, 1000.0);
                }

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
        GridFactory factory = new GridFactory(100,100);
        float[] vertexBufferData = factory.getVertexBuffer();
        int[] indexBufferData = factory.getIndexBuffer();

        // vertex binding description, concise version
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2), // 2 floats
        };

        buffers = new OGLBuffers(vertexBufferData, attributes, indexBufferData);
    }

    @Override
    public void init() throws IOException {
        OGLUtils.printOGLparameters();
        glClearColor(0f, 0f, 0f, 1.0f);

        createBuffers();

        shaderProgram = ShaderUtils.loadProgram("/myProject/myShader.vert",
                "/myProject/myShader.frag",
                null,null,null,null);

        // Shader program set
        glUseProgram(this.shaderProgram);

        texture = new OGLTexture2D("textures/globe.jpg");
        texture_n = new OGLTexture2D("textures/globeNormal.png");
        texture_h = new OGLTexture2D("textures/globeHeight.jpg");

        // internal OpenGL ID of a shader uniform (constant during one draw call
        // - constant value for all processed vertices or pixels) variable
        locProjection = glGetUniformLocation(shaderProgram, "projection");
        locView = glGetUniformLocation(shaderProgram, "view");
        locRotateX = glGetUniformLocation(shaderProgram, "rotateX");
        locLightPos = glGetUniformLocation(shaderProgram, "lightPos");
        locEyePos = glGetUniformLocation(shaderProgram,"eyePos");
        paramFunc = glGetUniformLocation(shaderProgram,"paramFunc");
        renderTexture = glGetUniformLocation(shaderProgram,"renderTexture");
        lightType = glGetUniformLocation(shaderProgram,"lightType");
        mappingType = glGetUniformLocation(shaderProgram,"mappingType");

        view = view.withPosition(new Vec3D(10, 10, 5))
                .withAzimuth(Math.PI * 1.25)
                .withZenith(Math.PI * -0.125);

        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glEnable(GL_DEPTH_TEST);

        textureViewer = new OGLTexture2D.Viewer();
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

        if(rotate){
            rotateValue -= 0.01;
        }

        rotateX = new Mat4RotZ(rotateValue);
        if(persp){
            glUniformMatrix4fv(locProjection, false, projPers.floatArray());
        }else{
            glUniformMatrix4fv(locProjection, false, projOrth.floatArray());
        }

        glUniformMatrix4fv(locView, false, view.getViewMatrix().floatArray());
        glUniformMatrix4fv(locRotateX, false, rotateX.floatArray());
        glUniform3f(locLightPos, (float) lightPos.getX(), (float) lightPos.getY(), (float) lightPos.getZ());

        glUniform1f(paramFunc, functionChanger);
        glUniform1f(renderTexture, textureToggle);
        glUniform1f(lightType, lightToggle);
        glUniform1f(mappingType, mappingToggle);

        if(fill){
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }


        texture.bind(shaderProgram, "texture", 0);
        texture_n.bind(shaderProgram, "normTex", 0);
        texture_h.bind(shaderProgram, "heightTex", 0);
        // bind and draw
        buffers.draw(GL_TRIANGLES, shaderProgram);

        textureViewer.view(texture, -1, -1, 0.5);

        textRenderer.clear();
        String controlText = "[LMB] camera, WSAD move, SHIFT up, L.CTRL down";
        String functionChangeText = "Change object shape by NUM 1-5";
        String fillText = "F for toggle fill";
        String rotateText = "R for toggle rotate";
        String projectionText = "P for toggle projection";
        String textureText = "T for toggle texture";
        textRenderer.addStr2D(3, 20, controlText);
        textRenderer.addStr2D(3, 40, functionChangeText);
        textRenderer.addStr2D(3, 60, fillText + ", " + rotateText + ", " + projectionText);
        textRenderer.addStr2D(3, 80, textureText);
        textRenderer.addStr2D(width-90, height-3, " (c) PGRF UHK");
        textRenderer.draw();
    }
}