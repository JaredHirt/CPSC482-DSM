package DSM;

class Main {
    public static void main(String[] args) {
        // Read and create initial sparse matrix from the California test file.
        final String matrixPath = "TestMatrices/California.mtx";

        try {
            SparseMatrix matrix = SparseMatrix.readFile(matrixPath);
            matrix.optimizeDSM();
            System.out.println("Optimization completed for " + matrixPath);
        } catch (java.io.IOException e) {
            System.err.println("Failed to read matrix file: " + matrixPath);
            e.printStackTrace();
        }
    }
}