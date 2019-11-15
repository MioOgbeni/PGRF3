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
    int shaderProgram, locProjection, locView, locModel, paramFunc, surfaceType, locObjectColor, locMoveInTime, locLightPos, locViewPos, locLightBulb, locLightType, locAscii, locSpotlight, locSpotDir, locMoon, locCoordsInTexture;

    // All shits for light shaders
    int shaderProgramLight, locMoveInTimeLight, locViewLight, locModelLight, locProjectionLight, paramFuncLight;

    // Rotation counter
    float rotateValue = 0;
    float moveInTime = 0.0f;
    Boolean moveInTimeUp = true;

    float functionChanger = 0;
    float surfaceToggle = 0;
    String surfaceToggleDescriptor = "set color from CPU";
    float lightType = 0;
    String lightTypeDescriptor = "perPixel";

    Boolean fill = true;
    Boolean rotate = true;
    Boolean strip = false;
    int ascii = 0;
    int spotlight = 1;
    int coordsInTexture = 0;

    int stripOrNot = GL_TRIANGLES;

    Mat4RotX rotateX = new Mat4RotX(0);
    Mat4RotY rotateY = new Mat4RotY(0);
    Mat4RotZ rotateZ = new Mat4RotZ(rotateValue);
    Mat4 model = new Mat4();

    Camera view = new Camera();
    Mat4Transl lightPosition;
    Camera viewLight = new Camera();

    Mat4 projPers = new Mat4PerspRH(Math.PI / 3, LwjglWindow.HEIGHT/(float)LwjglWindow.WIDTH, 2, 60);
    Mat4 projOrth = new Mat4OrthoRH( 10, 10, 2, 60);
    Boolean persp = true;


    Color objectColor = new Color(0,255,0);
    Color objectColor2 = new Color(0,0,255);

    OGLTexture2D texture;
    OGLTexture2D texture_height;
    OGLTexture2D texture_m;
    OGLTexture2D texture_m_height;
    OGLTexture2D.Viewer textureViewer;

    OGLRenderTarget renderTarget;

    float lightMoveValue = -1;
    boolean lightMoveUp = true;

    @Override
    public void init() throws IOException {
        glClearColor(0f, 0f, 0f, 1.0f);
        glEnable(GL_DEPTH_TEST);

        shaderProgram = ShaderUtils.loadProgram("/myProject/myShader.vert",
                "/myProject/myShader.frag",
                null,null,null,null);

        shaderProgramLight = ShaderUtils.loadProgram("/myProject/myShaderLight.vert",
                "/myProject/myShaderLight.frag",
                null,null,null,null);

        texture = new OGLTexture2D("textures/globe.jpg");
        texture_height = new OGLTexture2D("textures/globeNormal.png");
        texture_m = new OGLTexture2D("textures/moon.jpg");
        texture_m_height = new OGLTexture2D("textures/moonNormal.png");

        createBuffers(texture.getWidth(), texture.getHeight());

        view = new Camera()
                .withPosition(new Vec3D(10, 10, 5))
                .withAzimuth(5 / 4f * Math.PI)
                .withZenith(-1 / 5f * Math.PI)
                .withFirstPerson(false)
                .withRadius(6);

        viewLight = new Camera()
                .withPosition(new Vec3D(8,8,4))
                .withAzimuth(5 / 4f * Math.PI)
                .withZenith(-1 / 8f * Math.PI)
                .withRadius(6);
        viewLight = viewLight.left(5);
        lightPosition = new Mat4Transl(viewLight.getPosition().getX(), viewLight.getPosition().getY(), viewLight.getPosition().getZ());

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);

        textureViewer = new OGLTexture2D.Viewer();

        textRenderer = new OGLTextRenderer(width, height);

        renderTarget = new OGLRenderTarget(width, height);
    }

    @Override
    public void display() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);

        texture.bind(shaderProgram, "mainTex", 0);
        texture_height.bind(shaderProgram, "mainHighTex", 1);
        texture_m.bind(shaderProgram, "moonTex", 2);
        texture_m_height.bind(shaderProgram, "moonHighTex", 3);

        if(moveInTimeUp){
            moveInTime = moveInTime + 0.01f;
            if(moveInTime >= 0.6f){
                moveInTimeUp = false;
            }
        }else {
            moveInTime = moveInTime - 0.01f;
            if(moveInTime <= 0.0f){
                moveInTimeUp = true;
            }
        }

        if(lightMoveUp){
            lightMoveValue = lightMoveValue + 0.01f;
            viewLight = viewLight.right(0.05);
            if(lightMoveValue >= 1.0f){
                lightMoveUp = false;
            }
        }else {
            lightMoveValue = lightMoveValue - 0.01f;
            viewLight = viewLight.left(0.05);
            if(lightMoveValue <= -1.0f){
                lightMoveUp = true;
            }
        }

        lightPosition = new Mat4Transl(viewLight.getPosition().getX(), viewLight.getPosition().getY(), viewLight.getPosition().getZ());

        if(rotate){
            rotateValue -= 0.01;
        }
        rotateX = new Mat4RotX(0);
        rotateY = new Mat4RotY(0);
        rotateZ = new Mat4RotZ(rotateValue);
        model = rotateX.mul(rotateY.mul(rotateZ));

        glCullFace(GL_FRONT);
        renderFromLight();
        glCullFace(GL_BACK);
        renderFromViewer();

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        textureViewer.view(texture, -1, -1, 0.5);
        textureViewer.view(renderTarget.getDepthTexture(), -0.5, 0, -0.5);

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
        String lightTypeText = "L for toggle light mode";
        String coordsInTextureText = "J for toggle coords in texture rendering";
        String asciiText = "M for toggle ascii rendering";
        String spotlightText = "N for toggle spotlight rendering";
        textRenderer.addStr2D(3, 20, controlText);
        textRenderer.addStr2D(3, 40, functionChangeText);
        textRenderer.addStr2D(3, 60, String.format("%s (%b), %s (%b), %s (%b), %s (%b)", fillText, fill, rotateText, rotate, projectionText, persp, stripText, strip));
        textRenderer.addStr2D(3, 80, String.format("%s (%s %s)", surfaceTypeText, surfaceToggle, surfaceToggleDescriptor));
        textRenderer.addStr2D(3, 100, String.format("%s (%s %s)", lightTypeText, lightType, lightTypeDescriptor));
        textRenderer.addStr2D(3, 120, String.format("%s (%s %s)", coordsInTextureText, coordsInTexture, coordsInTexture!=0.0f));
        textRenderer.addStr2D(3, 140, String.format("%s (%s %s)", asciiText, ascii, ascii!=0.0f));
        textRenderer.addStr2D(3, 160, String.format("%s (%s %s)", spotlightText, spotlight, spotlight!=0.0f));
        textRenderer.addStr2D(width-90, height-3, " (c) PGRF UHK");
        textRenderer.draw();
    }

    public void renderFromLight(){
        //----------------------------------------------------From Light
        renderTarget.bind(); //bind light buffer

        glUseProgram(shaderProgramLight);   //setup shader program
        loadShaderLight(shaderProgramLight);
        glViewport(0,0, width, height); //setup window

        glClearColor(0f, 0f, 0f, 1.0f); //clear window
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //clear buffers

        //setup scene filling
        if(fill){
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        //setup scene projection
        if(persp){
            glUniformMatrix4fv(locProjectionLight, false, projPers.floatArray());
        }else{
            glUniformMatrix4fv(locProjectionLight, false, projOrth.floatArray());
        }

        //----------------------------------------------------Setup main object in scene
        glUniform1f(locMoveInTimeLight, moveInTime); //move him

        glUniformMatrix4fv(locViewLight, false, viewLight.getViewMatrix().floatArray()); //set view from light

        glUniformMatrix4fv(locModelLight, false, model.floatArray()); //set model for light

        glUniform1f(paramFuncLight, functionChanger); //his shape

        buffers.draw(stripOrNot, shaderProgramLight); //draw him

        //----------------------------------------------------Setup second object in scene
        glUniform1f(paramFuncLight, 0);
        glUniformMatrix4fv(locModelLight, false, new Mat4RotZ(rotateValue).mul(new Mat4Scale(0.3).mul(new Mat4Transl(3,3, 0).mul(new Mat4RotZ(-rotateValue)))).floatArray());
        buffers.draw(stripOrNot, shaderProgramLight); //draw him

        //----------------------------------------------------Setup base plane object
        glUniform1f(paramFuncLight, 10); //it fill be plane

        glUniformMatrix4fv(locModelLight, false, new Mat4Scale(10).mul(new Mat4Transl(-5,-5,-2)).floatArray()); //his position and scale

        buffers.draw(stripOrNot, shaderProgramLight); //draw him
    }

    public void renderFromViewer(){
        //----------------------------------------------------From Viewer
        glBindFramebuffer(GL_FRAMEBUFFER, 0); //change viewer buffer

        glUseProgram(shaderProgram); //setup shader program
        loadShader(shaderProgram);
        glViewport(0,0, width, height); //setup window

        glClearColor(0f, 0f, 0f, 1.0f); //clear window
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //clear buffers

        renderTarget.getDepthTexture().bind(shaderProgram, "shadowMap", 4); //bind light buffer like a viewer texture

        glUniform1i(locAscii,  ascii);
        glUniform1i(locSpotlight, spotlight);
        glUniform1i(locCoordsInTexture, coordsInTexture);
        glUniform3f(locSpotDir, (float) viewLight.getViewVector().getX(), (float) viewLight.getViewVector().getY(), (float) viewLight.getViewVector().getZ());

        //setup scene filling
        if(fill){
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }else{
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }

        //setup scene projection
        if(persp){
            glUniformMatrix4fv(locProjection, false, projPers.floatArray());
        }else{
            glUniformMatrix4fv(locProjection, false, projOrth.floatArray());
        }

        glUniform1f(locLightType, lightType);

        //vec3 test = new Mat4RotX(rotateValue).mul(viewLight.getPosition())
        glUniform3f(locLightPos, (float) viewLight.getPosition().getX(), (float) viewLight.getPosition().getY(), (float) viewLight.getPosition().getZ());
        glUniform3f(locViewPos, (float) view.getPosition().getX(), (float) view.getPosition().getY(), (float) view.getPosition().getZ());

        //----------------------------------------------------Setup main object in scene
        glUniform1i(locMoon, 0);
        glUniform1f(locMoveInTime, moveInTime); //move him

        glUniformMatrix4fv(locView, false, view.getViewMatrix().floatArray()); //set view from viewer

        glUniformMatrix4fv(locModel, false, model.floatArray()); //set model for viewer

        glUniformMatrix4fv(locViewLight, false, viewLight.getViewMatrix().floatArray()); //set view for shadow

        glUniform1f(paramFunc, functionChanger); //his shape
        glUniform1f(surfaceType, surfaceToggle); //his color
        glUniform3f(locObjectColor, objectColor.getRed(), objectColor.getGreen(), objectColor.getBlue()); //if color is <1 or 4< use this color from cpu

        buffers.draw(stripOrNot, shaderProgram); //draw him

        //----------------------------------------------------Setup second object in scene
        glUniform1i(locMoon, 1);
        glUniform1f(paramFunc, 0);
        glUniformMatrix4fv(locModel, false, new Mat4RotZ(rotateValue).mul(new Mat4Scale(0.3).mul(new Mat4Transl(3,3, 0).mul(new Mat4RotZ(-rotateValue)))).floatArray());
        buffers.draw(stripOrNot, shaderProgram); //draw him

        //----------------------------------------------------Setup base plane object
        glUniform1f(paramFunc, 10); //his shape
        glUniform1f(surfaceType, 10); //his color
        glUniform3f(locObjectColor, objectColor2.getRed(), objectColor2.getGreen(), objectColor2.getBlue()); //if color is <1 or 4< use this color from cpu
        glUniformMatrix4fv(locModel, false, new Mat4Scale(10).mul(new Mat4Transl(-5,-5,-2)).floatArray()); //his position and scale
        buffers.draw(stripOrNot, shaderProgram); //draw him

        //----------------------------------------------------Setup light bulb
        glUniform1i(locLightBulb, 1);
        glUniform1f(paramFunc, 0); //his shape
        glUniform1f(surfaceType, 10); //his color
        glUniform3f(locObjectColor, 200, 200, 0); //if color is <1 or 4< use this color from cpu
        glUniformMatrix4fv(locModel, false, new Mat4Scale(0.1).mul(lightPosition).floatArray()); //his position and scale
        buffers.draw(stripOrNot, shaderProgram); //draw him
        glUniform1i(locLightBulb, 0);
    }

    void createBuffers(int textureWidth, int textureHeight) {
        GridFactory factory = new GridFactory(10,10, textureWidth, textureHeight);
        float[] vertexBufferData = factory.getVertexBuffer();

        int[] indexBufferData;
        if(!strip){
            indexBufferData = factory.getIndexBuffer();
        }else{
            indexBufferData = factory.getIndexBufferStrip();
        }

        // vertex binding description, concise version
        OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib("inPosition", 2),
                new OGLBuffers.Attrib("inTexturePosition", 2)
        };

        buffers = new OGLBuffers(vertexBufferData, attributes, indexBufferData);
    }

    private void loadShader(int shader) {
        locView = glGetUniformLocation(shader, "view");
        locModel = glGetUniformLocation(shader, "model");
        locProjection = glGetUniformLocation(shader, "projection");
        paramFunc = glGetUniformLocation(shader,"paramFunc");
        surfaceType = glGetUniformLocation(shader,"surfaceType");
        locObjectColor = glGetUniformLocation(shader,"objectColor");
        locMoveInTime = glGetUniformLocation(shader,"moveInTime");
        locLightBulb = glGetUniformLocation(shader,"lightBulb");
        locLightType = glGetUniformLocation(shader, "lightType");
        locAscii = glGetUniformLocation(shader, "ascii");
        locSpotDir = glGetUniformLocation(shader, "spotDir");
        locMoon = glGetUniformLocation(shader, "moon");
        locSpotlight = glGetUniformLocation(shader, "spotlight");
        locCoordsInTexture = glGetUniformLocation(shader, "coordsInTexture");

        locViewLight = glGetUniformLocation(shader, "viewLight");
        locLightPos = glGetUniformLocation(shader, "lightPos");
        locViewPos = glGetUniformLocation(shader, "viewPos");
    }

    private void loadShaderLight(int shader) {
        locViewLight = glGetUniformLocation(shader, "view");
        locModelLight = glGetUniformLocation(shader, "model");
        locProjectionLight = glGetUniformLocation(shader, "projection");
        paramFuncLight = glGetUniformLocation(shader,"paramFunc");
        locMoveInTimeLight = glGetUniformLocation(shader,"moveInTime");
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
                            stripOrNot = GL_TRIANGLES;
                            createBuffers(texture.getWidth(), texture.getHeight());
                        }else{
                            strip = true;
                            stripOrNot = GL_TRIANGLE_STRIP;
                            createBuffers(texture.getWidth(), texture.getHeight());
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
                    case GLFW_KEY_L:
                        if(lightType == 0){
                            lightType = 1;
                            lightTypeDescriptor = "perVertex";
                        }else{
                            lightType = 0;
                            lightTypeDescriptor = "perPixel";
                        }
                        break;
                    case GLFW_KEY_M:
                        if(ascii == 0){
                            ascii = 1;
                        }else{
                            ascii = 0;
                        }
                        break;
                    case GLFW_KEY_N:
                        if(spotlight == 0){
                            spotlight = 1;
                        }else{
                            spotlight = 0;
                        }
                        break;
                    case GLFW_KEY_J:
                        if(coordsInTexture == 0){
                            coordsInTexture = 1;
                        }else{
                            coordsInTexture = 0;
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
                    projPers = new Mat4PerspRH(Math.PI / 3, height / (double) width, 2, 60);
                }else{
                    projOrth = new Mat4OrthoRH( 10, 10, 2, 60);
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