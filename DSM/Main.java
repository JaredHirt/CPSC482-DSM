package DSM;

class Main {
    public static void main(String[] args) {
        String[] matrixPaths;
        if (args != null && args.length > 0) {
            matrixPaths = args;
        } else {
            matrixPaths = new String[] {
                "TestMatrices/California.mtx",
                "TestMatrices/EPA.mtx",
                "TestMatrices/EVA.mtx",
                "TestMatrices/GD98_a.mtx",
                "TestMatrices/GD99_c.mtx",
                "TestMatrices/GlossGT.mtx",
                "TestMatrices/HEP-th-new.mtx",
                "TestMatrices/HEP-th.mtx",
                "TestMatrices/Tina_AskCal.mtx",
                "TestMatrices/wb-cs-stanford.mtx"
            };
        }

        for (String matrixPath : matrixPaths) {
            try {
                SparseMatrix sccOnly = SparseMatrix.readFile(matrixPath);
                SparseMatrix finalOptimized = SparseMatrix.readFile(matrixPath);

                sccOnly.reduceFeedback();
                finalOptimized.optimizeDSM();

                long sccLoss = sccOnly.calculateLoss();
                long finalLoss = finalOptimized.calculateLoss();
                long difference = sccLoss - finalLoss;

                System.out.println("----------------------------------------");
                System.out.println("Matrix: " + matrixPath);
                System.out.println("Loss after SCC ordering: " + sccLoss);
                System.out.println("Final loss after full optimization: " + finalLoss);
                System.out.println("Loss difference (SCC - final): " + difference);
            } catch (java.io.IOException e) {
                System.err.println("Failed to read matrix file: " + matrixPath);
                e.printStackTrace();
            }
        }
    }
}