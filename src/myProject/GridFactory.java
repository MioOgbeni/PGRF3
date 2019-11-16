package myProject;

public class GridFactory {
    float[] vertexBuffer;
    int[] indexBuffer;

    int[] indexBufferStrip;

    public GridFactory(){

    }

    public GridFactory(int xSize, int ySize, int width, int height){
        createVB(xSize, ySize, width, height);
        createIBList(xSize, ySize);
        createIBStrip(xSize, ySize);
    }

    private void createVB(int xSize, int ySize, int width, int height) {
        vertexBuffer = new float[2 * ySize * xSize * 2];
        int index = 0;
        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                vertexBuffer[index++] = (float) x / (xSize - 1);
                vertexBuffer[index++] = (float) y / (ySize - 1);
                vertexBuffer[index++] = (float) x / (xSize - 1);
                vertexBuffer[index++] = (float) y / (ySize - 1);
            }
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

    private int[] createIBStrip(int xSize, int ySize)
    {
        indexBufferStrip = new int[(xSize) * (ySize) + (xSize * (ySize-2)) + (2*(ySize-1))];
        int index = 0;
        for (int j = 0; j < ySize - 1; j+=1)
        {
            int row = 0;
            for (int i = 0; i <= xSize - 1; i+=1)
            {
                indexBufferStrip[index++] = j * xSize + i;
                indexBufferStrip[index++] = (j + 1) * xSize + i;

                row = i;
            }
            indexBufferStrip[index++] = (j + 1) * xSize + row;
            indexBufferStrip[index++] = (j + 1) * xSize;

        }

        return indexBufferStrip;
    }

    public float[] getVertexBuffer() {
        return vertexBuffer;
    }

    public int[] getIndexBuffer() {
        return indexBuffer;
    }

    public int[] getIndexBufferStrip() {
        return indexBufferStrip;
    }
}
