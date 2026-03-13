/**
 * A class representing a sparse matrix, which is a matrix that contains mostly zero values.
 * This class stores a binary Sparse matrix (values are either 0 or 1) in CRS format.
 * 
 */
package DSM;
public class SparseMatrix {
    private int[] columnIndex;
    private int[] rowPointer;
}


public SparseMatrix(int rows, int columns, int elements) {
}

/**
 * Transposes
 */
public SparseMatrix transpose(){
    //todo
    return this;
}