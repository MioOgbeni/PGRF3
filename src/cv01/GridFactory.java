package cv01;

import lwjglutils.OGLBuffers;

public class GridFactory {
    float[] vertexBuffer;
    int[] indexBuffer;
    int[] indexBufferStrip;

    public GridFactory(){

    }

    public GridFactory(int xSize, int ySize){
        createVB(xSize, ySize);
        createIBList(xSize, ySize);
        createIBStrip(xSize, ySize);
    }

    private void createVB(int xSize, int ySize) {
        vertexBuffer = new float[2 * ySize * xSize];
        int index = 0;
        for (int y = 0; y < ySize; y++)
            for (int x = 0; x < xSize; x++) {
                vertexBuffer[index ++] = (float) x / (xSize - 1);
                vertexBuffer[index ++] = (float) y / (ySize - 1);
            }
    }

    private void createIBList(int xSize, int ySize){
        indexBuffer = new int[6 * (ySize - 1) * (xSize - 1)];
        int index = 0;
        for (int y = 0; y < ySize - 1; y++) {
            for (int x = 0; x < xSize - 1; x++) {
                indexBuffer[index++] = y * xSize + x;
                indexBuffer[index++] = y * xSize + x + 1;
                indexBuffer[index++] = (y + 1) * xSize + x + 1;

                indexBuffer[index++] = (y + 1) * xSize + x + 1;
                indexBuffer[index++] = y * xSize + x;
                indexBuffer[index++] = (y + 1) * xSize + x;
            }
        }
    }

    private void createIBStrip(int xSize, int ySize){
        indexBufferStrip = new int[6 * (ySize - 1) * (xSize - 1)];
        int index = 0;
        for (int y = 0; y < ySize - 1; y+=2) {
            int xHelp = 0;
            for (int x = 0; x < xSize - 1; x+=2) {
                indexBufferStrip[index++] = y * xSize + x;
                indexBufferStrip[index++] = (y + 1) * xSize + x;
                indexBufferStrip[index++] = y * xSize + x + 1;
                indexBufferStrip[index++] = (y + 1) * xSize + x + 1;
                xHelp = x;
            }
            indexBufferStrip[index++] = y * xSize + xHelp;
            indexBufferStrip[index++] = (y + 1) * xSize + xHelp;
        }
    }

    public float[] getVertexBuffer() {
        return vertexBuffer;
    }

    public int[] getIndexBuffer() {
        return indexBuffer;
    }
}
