package DSM;
/**
 * A class representing a sparse matrix, which is a matrix that contains mostly zero values.
 * This class stores a binary Sparse matrix (values are either 0 or 1) in CRS format.
 * 
 */


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class SparseMatrix {
    private int[] rowPointer;
    private int[] columnIndex;

    private int columns;
    private int rows;
    private int elements;


    public SparseMatrix(int rows, int columns, int elements) {
        columnIndex = new int[elements];
        rowPointer = new int[rows + 1];
        this.columns = columns;
        this.rows = rows;
        this.elements = elements;
    }


    public static SparseMatrix readFile(String filepath) throws java.io.IOException {

        FileReader fr = null;
        // try to assign the file
        try {
            fr = new FileReader(filepath);
        } catch (FileNotFoundException ignored) {
            System.out.println("Couldn't find the file");
            System.exit(1);
        }
        SparseMatrix sm;
        try (BufferedReader br = new BufferedReader(fr)) {

            // get past all the comments
            String line;

            // Skip comments/blank lines and stop at the size line.
            while (true) {
                line = br.readLine();
                if (line == null) {
                    throw new IOException("Unexpected end of file before matrix size line.");
                }
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("%")) {
                    break;
                }
            }

            // should be on size line now
            // read size and elements
            String[] data = line.split("\\s+");
            if (data.length < 3) {
                throw new IOException("Invalid Matrix Market size line: " + line);
            }
            int row = Integer.parseInt(data[0]);
            int col = Integer.parseInt(data[1]);
            int ele = Integer.parseInt(data[2]);
            assert row == col : "Matrix is not square, cannot be a DSM";
            sm = new SparseMatrix(row, col, ele);

            // start reading elements in
            int[][] allElements = new int[ele][2];
            int allElementIndex = 0;
            // read all non-zero entries
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("%")) {
                    continue;
                }

                data = line.split("\\s+");
                if (data.length < 2) {
                    throw new IOException("Invalid element line: " + line);
                }

                int r = Integer.parseInt(data[0]);
                int c = Integer.parseInt(data[1]);

                if (r < 1 || r > row || c < 1 || c > col) {
                    throw new IOException("Element index out of bounds: (" + r + ", " + c + ")");
                }
                if (allElementIndex >= ele) {
                    throw new IOException("More elements than declared in header: " + ele);
                }

                allElements[allElementIndex++] = new int[]{
                    r,
                    c
                };
            }

            if (allElementIndex != ele) {
                throw new IOException("Element count mismatch. Expected " + ele + " but read " + allElementIndex);
            }

            // TODO: change the sorting method if Dr. Hossain doesnt like us doing this
            // sort to ease the process of putting in row-major form
            Arrays.sort(allElements, Comparator.comparingInt((int[] x) -> x[0]).thenComparingInt(x -> x[1]));

            // add to matrix
            int i = 0;
            int j = 0;
            for (int[] element : allElements){
                // subtract one from both coordinates so it's 0-indexed internally
                sm.columnIndex[i] = element[1] - 1;
                // go to next row if row number is bigger than the last
                // this for loop only executes if element[0] > j
                for (; j<element[0]; j++)
                    sm.rowPointer[j] = i;
                i++;
            }
            // Fill all remaining rows so trailing empty rows are represented correctly in CSR.
            for (; j <= row; j++)
                sm.rowPointer[j] = ele;
        }

        return sm;
    }

    public int[] getColumnIndex() { return columnIndex; }
    public int[] getRowPointer() { return rowPointer; }

    /**
     * Transposes
     */
    public SparseMatrix transpose(){
        //todo
        SparseMatrix sm = new SparseMatrix(this.columns, this.rows, this.elements);
        int[][] expandedStorage = new int[this.elements][2];
        // convert to standard sparse matrix storage
        int ind=0;
        for (int row=0; row<this.rows; row++) {
            for (; ind<rowPointer[row + 1]; ind++) {
                expandedStorage[ind] = new int[]{row, columnIndex[ind]};
            }
        }

        // switch rows and columns
        // fast transpose

        // counting array
        // count the terms
        int[] colCount = new int[this.columns];
        for (int i : columnIndex) {
            colCount[i]++;
        }
        // calculate column starts
        int[] startPos = new int[this.columns];
        for (int i=1; i<this.columns; i++) {
            startPos[i] = startPos[i-1]+colCount[i-1];
        }
        // transpose elements
        int[][] transposedStorage = new int[this.elements][2];
        for (int i=0; i<this.columnIndex.length; i++) {
            int col = expandedStorage[i][1];
            int pos = startPos[col];
            transposedStorage[pos] = new int[]{expandedStorage[i][1] + 1, expandedStorage[i][0] + 1};
            startPos[col]++;
        }


        // convert back to crs
        // add to matrix
        int i = 0;
        int j = 0;
        for (int[] element : transposedStorage){
            // subtract one from both coordinates so it's 0-indexed internally
            sm.columnIndex[i] = element[1] - 1;
            // go to next row if row number is bigger than the last
            // this for loop only executes if element[0] > j
            for (; j<element[0]; j++)
                sm.rowPointer[j] = i;
            i++;
        }
        for (; j <= sm.rows; j++)
            sm.rowPointer[j] = transposedStorage.length;

        return sm;
    }

    /**
     * Optimizes the sparse matrix by reducing the number of feedback loops and then the distance of the feedback loops from the main diagonal.
     * This algorithm does not find a perfect solution but instead finds a local minimum in a reasonable amount of time as the perfect solution is NP hard.
     */
    public void optimizeDSM(){
        reduceFeedback();
        minimizeDistanceFromDiagonal();
    }

    /**
     * Reduces the number of feedback loops in the matrix. This is the global minimum.
     */
    public void reduceFeedback(){
        //Get the transpose of the matrix
        SparseMatrix transposed = this.transpose();
        //Perform DFS on the original matrix putting the equivalent nodes of the transposed matrix into a stack



        //First we instantiate all of the nodes
        Node[] nodes = new Node[this.rows];
        for (int i=0; i<this.rows; i++) {
            nodes[i] = new Node(i);
        }



        //Now we created a stack of nodes. This will correctly order them by finish time.
        Stack nodeStack = new Stack<Node>();

        //Perform DFS on the original matrix. Once a node is finished, push it onto the stack
        
        for (Node node : nodes) {
            if (!node.visited) {
                dfs(node, this, nodes, nodeStack);
            }
        }
        //Perform DFS on the transposed matrix in stack order. Every node which is visited is part of a SCC and we can order them as such.
        ArrayList<Node> nodeOrder = new ArrayList<Node>(this.rows);
        while (!nodeStack.isEmpty()) {
            reorderSCC( (Node) nodeStack.pop(), transposed, nodes, nodeOrder);
        }

        //We now have an arraylist of nodes optimized for the number of feedback loops.
        //We can now create a map of the old index to the new index for each node. This will be used to reorder the matrix.
        int[] indexMap = new int[this.rows];
        for (int i=0; i<nodeOrder.size(); i++) {
            indexMap[nodeOrder.get(i).index] = i;
        }
        //We now must replace the existing ordering of elements in the matrix with the new ordering. We do this by recreating the matrix internally. Multiplication is far too expensive so we essentially create a new matrix.
        int[] newColumnIndex = new int[this.elements];
        int[] newRowPointer = new int[this.rows + 1];

        // Build CSR row starts for the reordered node sequence.
        newRowPointer[0] = 0;
        for (int i = 0; i < nodeOrder.size(); i++) {
            Node node = nodeOrder.get(i);
            int rowLength = rowPointer[node.index + 1] - rowPointer[node.index];
            newRowPointer[i + 1] = newRowPointer[i] + rowLength;
        }
        //Now we have the new row pointers, we can set up the new column index by going through each node in the new order and adding its column index to the new column index array.
        for(int i = 0; i < nodeOrder.size(); i++) {
            Node node = nodeOrder.get(i);
            for(int j = rowPointer[node.index]; j < rowPointer[node.index + 1]; j++) {
                newColumnIndex[newRowPointer[i] + (j - rowPointer[node.index])] = indexMap[columnIndex[j]];
            }
        }

        //This will result with the DSM being optimized for the number of feed back loops
        this.columnIndex = newColumnIndex;
        this.rowPointer = newRowPointer;
    }

    /**
     * DFS which puts the finished node onto the stack that is passed. This is topologically sorted.
     */
    private void dfs(Node node, SparseMatrix matrix, Node[] nodes, Stack<Node> nodeStack){
        //Base case if already visited
        if (node.visited) return;
        //Mark as visited
        node.visited = true;
        //Call dfs on all its neighbors
        for (int i = matrix.rowPointer[node.index]; i < matrix.rowPointer[node.index+1]; i++) {
            dfs(nodes[matrix.columnIndex[i]], matrix, nodes, nodeStack);
        }
        //Now the node is finished being visited. We push the node onto the stack.
        nodeStack.push(node);
    }

    /**
     * Performs DFS on the transposed matrix in stack order. Every node which is visited is part of a SCC and we can order them as such.
     * This will result with the DSM being optimized for the number of feed back loops
     * This takes advantage of the fact that every node is marked as visited from the first DFS. This means we can mark flip the boolean upon finishing it which will mark all nodes as unvisited again.
     */
    private void reorderSCC(Node node, SparseMatrix tranSparseMatrix, Node[] nodes, ArrayList<Node> nodeOrder){
        //Base case if visited by DFS but not reorderSCC
        if (!node.visited) return;
        //Mark as unvisited to mark that we have visited it in this DFS
        node.visited = false;
        //Call dfs on all its neighbors
        for (int i = tranSparseMatrix.rowPointer[node.index]; i < tranSparseMatrix.rowPointer[node.index+1]; i++) {
            reorderSCC(nodes[tranSparseMatrix.columnIndex[i]], tranSparseMatrix, nodes, nodeOrder);
        }
        //Now the node is finished being visited. We add the node to the end of the new order.
        nodeOrder.add(node);
    }

    /**
     * Reduces the distance of feedback loops from the main diagonal. This finds a local minimum as the perfect solution is NP hard.
     */
    public void minimizeDistanceFromDiagonal(){
        //Work now needs to be performed on reducing the distance of feedback loops from the main diagonal. 
        //The perfect solution for this is exponential, but we will perform an approximate solution by using Neural sort.
        //Neural sort is a differentiable sorting algorithm that can be used to optimize the order of elements in a list.
        //This is done by relaxing the constraint of elements being in a specific order and instead allowing them to be in a continuous space.
        //This allows us to use gradient descent to optimize the order of elements in the list.
        //This is done relative to a loss function that we define. This will be (large number * num_feedback_loops) + distance_from_diagonal
    }

        class Node {
        int index;
        boolean visited;

        public Node(int index) {
            this.index = index;
            this.visited = false;
        }
    }
    
    
}
