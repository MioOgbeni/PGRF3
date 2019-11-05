package myProject;


import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.awt.*;
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
    int shaderProgram, locProjection, locView, locModel, paramFunc, surfaceType, locObjectColor;

    // All shits for light shaders
    int shaderProgramLight, locLightVP, locViewLight, locModelLight, locProjectionLight, paramFuncLight;

    // Rotation counter
    float rotateValue = 0;
    float lightRotateValue = 0;

    float functionChanger = 0;
    float surfaceToggle = 0;
    String surfaceToggleDescriptor = "set color from CPU";
    Boolean fill = true;
    Boolean rotate = true;
    Boolean strip = false;

    Mat4RotX rotateX = new Mat4RotX(0);
    Mat4RotY rotateY = new Mat4RotY(0);
    Mat4RotZ rotateZ = new Mat4RotZ(rotateValue);
    Mat4 model = new Mat4();


    Camera view = new Camera();
    Mat4Transl lightPosition;
    Camera viewLight = new Camera();

    Mat4 projPers = new Mat4PerspRH(Math.PI / 3, LwjglWindow.HEIGHT/(float)LwjglWindow.WIDTH, 1, 200);
    Mat4 projOrth = new Mat4OrthoRH( 10, 10, 1, 200);
    Boolean persp = true;


    Color objectColor = new Color(0,255,0);
    Color objectColor2 = new Color(0,0,255);

    OGLTexture2D texture;
    OGLTexture2D texture_n;
    OGLTexture2D texture_h;
    OGLTexture2D.Viewer textureViewer;

    OGLRenderTarget renderTarget;

    @Override
    public void init() throws IOException {
        glClearColor(0f, 0f, 0f, 1.0f);
        glEnable(GL_DEPTH_TEST);

        createBuffers();

        shaderProgram = ShaderUtils.loadProgram("/myProject/myShader.vert",
                "/myProject/myShader.frag",
                null,null,null,null);

        shaderProgramLight = ShaderUtils.loadProgram("/myProject/myShaderLight.vert",
                "/myProject/myShaderLight.frag",
                null,null,null,null);

        glUseProgram(shaderProgram);

        texture = new OGLTexture2D("textures/globe.jpg");
        texture_n = new OGLTexture2D("textures/globeNormal.png");
        texture_h = new OGLTexture2D("textures/globeHeight.jpg");

        // internal OpenGL ID of a shader uniform (constant during one draw call
        // - constant value for all processed vertices or pixels) variable
        loadShader(shaderProgram);
        loadShaderLight(shaderProgramLight);

        view = new Camera()
                .withPosition(new Vec3D(10, 10, 5))
                .withAzimuth(5 / 4f * Math.PI)
                .withZenith(-1 / 5f * Math.PI)
                .withFirstPerson(false)
                .withRadius(6);

        viewLight = new Camera()
                .withPosition(new Vec3D(6,6,4))
                .withAzimuth(5 / 4f * Math.PI)
                .withZenith(-1 / 5f * Math.PI);
        lightPosition = new Mat4Transl(viewLight.getPosition().getX(), viewLight.getPosition().getY(), viewLight.getPosition().getZ());

        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glEnable(GL_DEPTH_TEST);

        textureViewer = new OGLTexture2D.Viewer();
        textRenderer = new OGLTextRenderer(width, height);

        renderTarget = new OGLRenderTarget(width, height);
    }

    @Override
    public void display() {
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glEnable(GL_DEPTH_TEST);

        texture.bind(shaderProgram, "mainTex", 0);
        texture_n.bind(shaderProgram, "normTex", 1);
        texture_h.bind(shaderProgram, "heightTex", 2);

        renderTarget.bind();
        renderFromLight();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        renderTarget.getDepthTexture().bind(shaderProgram, "shadowMap", 3);
        renderFromViewer();

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        textureViewer.view(texture, -1, -1, 0.5);
        textureViewer.view(renderTarget.getDepthTexture(), -1, -0.5, 0.5);
        if(fill){
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        textRenderer.clear();
        String controlText = "[LMB] camera, WSAD move, SHIFT up, L.CTRL down";
        String functionChangeText = "Change object shape by NUM 1-5";
        String fillText = "F for toggle fill";
        String rotateText = "R for toggle rotation";
        String projectionText = "P for toggle perspective";
        String surfaceTypeText = "K for toggle surface type";
        String stripText = "O for toggle IB strip";
        textRenderer.addStr2D(3, 20, controlText);
        textRenderer.addStr2D(3, 40, functionChangeText);
        textRenderer.addStr2D(3, 60, String.format("%s (%b), %s (%b), %s (%b), %s (%b)", fillText, fill, rotateText, rotate, projectionText, persp, stripText, strip));
        textRenderer.addStr2D(3, 80, String.format("%s (%s %s)", surfaceTypeText, surfaceToggle, surfaceToggleDescriptor));
        textRenderer.addStr2D(width-90, height-3, " (c) PGRF UHK");
        textRenderer.draw();
    }

    private void loadShader(int shader) {
        locView = glGetUniformLocation(shader, "view");
        locModel = glGetUniformLocation(shader, "model");
        locProjection = glGetUniformLocation(shader, "projection");
        paramFunc = glGetUniformLocation(shader,"paramFunc");
        surfaceType = glGetUniformLocation(shader,"surfaceType");
        locObjectColor = glGetUniformLocation(shader,"objectColor");
        locLightVP = glGetUniformLocation(shader, "lightViewProjection");

        locViewLight = glGetUniformLocation(shader, "viewLight");
        locModelLight = glGetUniformLocation(shader, "modelLight");
        locProjectionLight = glGetUniformLocation(shader, "projectionLight");
    }

    private void loadShaderLight(int shader) {
        locViewLight = glGetUniformLocation(shader, "view");
        locModelLight = glGetUniformLocation(shader, "model");
        locProjectionLight = glGetUniformLocation(shader, "projection");
        paramFuncLight = glGetUniformLocation(shader,"paramFunc");
    }

    public void renderFromLight(){
        glUseProgram(shaderProgramLight);
        glViewport(0,0, width, height);

        glClearColor(0f, 0f, 0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if(fill){
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        if(rotate){
            rotateValue -= 0.01;
        }

        if(persp){
            glUniformMatrix4fv(locProjectionLight, false, projPers.floatArray());
        }else{
            glUniformMatrix4fv(locProjectionLight, false, projOrth.floatArray());
        }

        glUniformMatrix4fv(locViewLight, false, viewLight.getViewMatrix().floatArray());

        rotateX = new Mat4RotX(0);
        rotateY = new Mat4RotY(0);
        rotateZ = new Mat4RotZ(rotateValue);
        model = rotateX.mul(rotateY.mul(rotateZ));
        glUniformMatrix4fv(locModelLight, false, model.floatArray());

        glUniform1f(paramFuncLight, functionChanger);

        buffers.draw(GL_TRIANGLES, shaderProgramLight);

        glUniform1f(paramFuncLight, 10);
        glUniformMatrix4fv(locModelLight, false, new Mat4Scale(10).mul(new Mat4Transl(-5,-5,-2)).floatArray());
        buffers.draw(GL_TRIANGLES, shaderProgramLight);
    }

    public void renderFromViewer(){
        glUseProgram(shaderProgram);
        glViewport(0,0, width, height);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glClearColor(0f, 0f, 0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if(fill){
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        if(rotate){
            rotateValue -= 0.01;
        }

        if(persp){
            glUniformMatrix4fv(locProjection, false, projPers.floatArray());
        }else{
            glUniformMatrix4fv(locProjection, false, projOrth.floatArray());
        }

        glUniformMatrix4fv(locView, false, view.getViewMatrix().floatArray());

        rotateX = new Mat4RotX(0);
        rotateY = new Mat4RotY(0);
        rotateZ = new Mat4RotZ(rotateValue);
        model = rotateX.mul(rotateY.mul(rotateZ));
        glUniformMatrix4fv(locModel, false, model.floatArray());

        if(persp){
            glUniformMatrix4fv(locProjectionLight, false, projPers.floatArray());
        }else{
            glUniformMatrix4fv(locProjectionLight, false, projOrth.floatArray());
        }
        glUniformMatrix4fv(locViewLight, false, viewLight.getViewMatrix().floatArray());
        glUniformMatrix4fv(locModelLight, false, model.floatArray());

        glUniform1f(paramFunc, functionChanger);
        glUniform1f(surfaceType, surfaceToggle);
        glUniform3f(locObjectColor, objectColor.getRed(), objectColor.getGreen(), objectColor.getBlue());

        buffers.draw(GL_TRIANGLES, shaderProgram);

        glUniform1f(paramFunc, 10);
        glUniform1f(surfaceType, 10);
        glUniform3f(locObjectColor, objectColor2.getRed(), objectColor2.getGreen(), objectColor2.getBlue());
        glUniformMatrix4fv(locModel, false, new Mat4Scale(10).mul(new Mat4Transl(-5,-5,-2)).floatArray());
        buffers.draw(GL_TRIANGLES, shaderProgram);

        glUniform1f(paramFunc, 0);
        glUniform1f(surfaceType, 10);
        glUniform3f(locObjectColor, 255, 255, 0);
        lightRotateValue += 0.01;
        glUniformMatrix4fv(locModel, false, new Mat4Scale(0.1).mul(lightPosition).floatArray());
        buffers.draw(GL_TRIANGLES, shaderProgram);
    }

    void createBuffers() {
        GridFactory factory = new GridFactory(100,100);
        float[] vertexBufferData = factory.getVertexBuffer();

        int[] indexBufferData;
        if(!strip){
            indexBufferData = factory.getIndexBuffer();
        }else{
            indexBufferData = factory.getIndexBufferStrip();
        }

        // vertex binding description, concise version
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2), // 2 floats
        };

        buffers = new OGLBuffers(vertexBufferData, attributes, indexBufferData);
    }

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
                    case GLFW_KEY_6:
                        functionChanger = 5;
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
                    case GLFW_KEY_O:
                        if(strip){
                            strip = false;
                        }else{
                            strip = true;
                        }
                        break;
                    case GLFW_KEY_K:
                        if(surfaceToggle == 0){
                            surfaceToggle = 1;
                            surfaceToggleDescriptor = "texture";
                        }else if (surfaceToggle == 1){
                            surfaceToggle = 2;
                            surfaceToggleDescriptor = "normal color";
                        }else if (surfaceToggle == 2){
                            surfaceToggle = 3;
                            surfaceToggleDescriptor = "xyz color";
                        }else if (surfaceToggle == 3){
                            surfaceToggle = 4;
                            surfaceToggleDescriptor = "depth color";
                        }else{
                            surfaceToggle = 0;
                            surfaceToggleDescriptor = "set color from CPU";
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
                    projPers = new Mat4PerspRH(Math.PI / 3, height / (double) width, 1, 200);
                }else{
                    projOrth = new Mat4OrthoRH( 10, 10, 1, 200);
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

}