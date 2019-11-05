package myProject;

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

    private int[] createIBStrip(int xSize, int ySize)
    {
        int naSudej = ySize % 2 + ySize;
        indexBufferStrip = new int[(((ySize * 4) + 2) * (naSudej / 2) - (ySize * 2 + 2)) - (ySize % 2 * (ySize * 2))];
        int index2 = 0;
        boolean smerGenerovani = true;
        for (int j = 0; j < ySize - 1; j++)
        {
            int pom = 0;

            if(smerGenerovani)
            {
                for (int i = 0; i < xSize; i++)
                {
                    indexBufferStrip[index2++] = (j * xSize) + i;
                    indexBufferStrip[index2++] = ((j + 1) * xSize) + i;
                    pom = i;
                }
                if (j < (ySize - 2))
                {
                    indexBufferStrip[index2++] = ((j + 1) * xSize) + (pom);
                    indexBufferStrip[index2++] = ((j + 1) * xSize) + (pom);
                }
            } else {
                for (int i = (xSize - 1); i > -1; i --)
                {
                    indexBufferStrip[index2++] = ((j + 1) * xSize) + i;
                    indexBufferStrip[index2++] = (j * xSize) + i;
                }
            }
            smerGenerovani = !smerGenerovani;
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
