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
    private char[] columnConfiguration;

    private int columns;
    private int rows;
    private int elements;


    public SparseMatrix(int rows, int columns, int elements) {
        columnIndex = new int[elements];
        rowPointer = new int[rows + 1];
        this.columns = columns;
        this.rows = rows;
        this.elements = elements;
        this.columnConfiguration = new char[rows];
        for (int i=0; i<rows; i++) {
            char c = (char)(i+65);
            columnConfiguration[i] = c;
        }
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
    public char[] getColumnConfiguration() { return columnConfiguration; }

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

        // Reverse SCC order so inter-SCC edges tend to fall below the diagonal (fewer col > row entries).
        for (int left = 0, right = nodeOrder.size() - 1; left < right; left++, right--) {
            Node tmp = nodeOrder.get(left);
            nodeOrder.set(left, nodeOrder.get(right));
            nodeOrder.set(right, tmp);
        }

        //We now have an arraylist of nodes optimized for the number of feedback loops.
        //We can now create a map of the old index to the new index for each node. This will be used to reorder the matrix.
        int[] indexMap = new int[this.rows];
        char[] newColumnConfiguration = new char[this.rows];
        for (int i=0; i<nodeOrder.size(); i++) {
            indexMap[nodeOrder.get(i).index] = i;
            newColumnConfiguration[nodeOrder.get(i).index] = columnConfiguration[i];
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
        this.columnConfiguration = newColumnConfiguration;
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
        //The perfect solution for this is exponential, but we will perform an approximate solution by using a standard genetic algorithm.
        //population of 105, we initialize it with copies of the current matrix with all but one having between 1-5 nodes swapped.
        //We keep the 5 best nodes and create 100 children by randomly swapping between 1-5 pairs of nodes in the parent.
        //We repeat until there is no improvement for 100 generations.
        // The time complexity per iteration is O(pop_size * (fitness + mutation)), where fitness is O(elements) and mutation is O(rows).

        // Create array of potential solutions.
        int[][] population = new int[105][this.rows];

        //For initial population we create 105 copies of the current matrix. With every solution but solution 0 having between 1-5 swaps.
        population[0] = new int[this.rows];

        //Copy of the current solution
        for(int i = 0; i < this.rows; i++){
            population[0][i] = i;
        }

        //Create the rest of the population with random swap mutations
        for(int i = 1; i < population.length; i++){
            population[i] = Arrays.copyOf(population[0], this.rows);
            int swaps = (int) (Math.random() * 5) + 1;
            for(int j = 0; j < swaps; j++){
                int index1 = (int) (Math.random() * this.rows);
                int index2 = (int) (Math.random() * this.rows);
                // swap
                int temp = population[i][index1];
                population[i][index1] = population[i][index2];
                population[i][index2] = temp;
            }
        }


        //Now that we have initialized our population, we can loop until we have no improvement for 100 generations. We will keep track of the best solution and its loss to determine when to stop.
        int[] bestSolution = Arrays.copyOf(population[0], this.rows);
        // Evaluate initial solution
        long bestLoss = reorderMatrix(population[0]).calculateLoss();
        int generationsWithoutImprovement = 0;
        while (generationsWithoutImprovement < 100) {
            //Calculate the loss for each solution and track the 5 best in O(1) per solution.
            long[] best5Losses = new long[5];
            int[] best5Indices = new int[5];
            long worstOf5 = Long.MAX_VALUE;
            int worstOf5Slot = 0;

            // Initialize best5 with max values
            for(int i = 0; i < 5; i++){
                best5Losses[i] = Long.MAX_VALUE;
                best5Indices[i] = -1;
            }

            // Track best 5 as we evaluate population
            for(int i = 0; i < population.length; i++){
                long loss = reorderMatrix(population[i]).calculateLoss();

                // If this loss is better than the worst of the top 5, insert it
                if(loss < worstOf5){
                    best5Losses[worstOf5Slot] = loss;
                    best5Indices[worstOf5Slot] = i;

                    // Recompute worst of 5 for next iteration
                    worstOf5 = best5Losses[0];
                    worstOf5Slot = 0;
                    for(int j = 1; j < 5; j++){
                        if(best5Losses[j] > worstOf5){
                            worstOf5 = best5Losses[j];
                            worstOf5Slot = j;
                        }
                    }
                }
            }

            //Keep the 5 best solutions and create 100 children by randomly swapping between 1-5 pairs of nodes in the parent.
            int[][] newPopulation = new int[population.length][this.rows];
            for(int i = 0; i < 5; i++){
                newPopulation[i] = Arrays.copyOf(population[best5Indices[i]], this.rows);
            }
            for(int i = 5; i < population.length; i++){
                newPopulation[i] = Arrays.copyOf(population[best5Indices[i % 5]], this.rows);
                int swaps = (int) (Math.random() * 5) + 1;
                for(int j = 0; j < swaps; j++){
                    int index1 = (int) (Math.random() * this.rows);
                    int index2 = (int) (Math.random() * this.rows);
                    // swap
                    int temp = newPopulation[i][index1];
                    newPopulation[i][index1] = newPopulation[i][index2];
                    newPopulation[i][index2] = temp;
                }
            }
            //Check if we have a new best solution. If so, reset generations without improvement. If not, increment it.
            long bestOfGeneration = best5Losses[0];
            for(int i = 1; i < 5; i++){
                if(best5Losses[i] < bestOfGeneration){
                    bestOfGeneration = best5Losses[i];
                }
            }
            if (bestOfGeneration < bestLoss) {
                bestLoss = bestOfGeneration;
                int bestIdx = 0;
                for(int i = 1; i < 5; i++){
                    if(best5Losses[i] < best5Losses[bestIdx]){
                        bestIdx = i;
                    }
                }
                bestSolution = Arrays.copyOf(population[best5Indices[bestIdx]], this.rows);
                generationsWithoutImprovement = 0;
            } else {
                generationsWithoutImprovement++;
            }

            population = newPopulation;
        }

        //Now we have the best solution we found. We can reorder the matrix to this solution.
        SparseMatrix optimized = reorderMatrix(bestSolution);
        this.columnIndex = optimized.columnIndex;
        this.rowPointer = optimized.rowPointer;
        this.columnConfiguration = optimized.columnConfiguration;
    }


    /**
     * Gives a sparse matrix based on a new ordering of the nodes.
     * newOrder is an array of the new order. A 3 at index 0 means that the node at index 0 should go to index 3 in the new matrix.
     */
    private SparseMatrix reorderMatrix(int[] newOrder) {
        // Note that newOrder is essentially a map old index -> new index.

        // Create a new sparse matrix that is a copy of the current one.
        SparseMatrix sm = new SparseMatrix(this.rows, this.columns, this.elements);
        for (int i=0; i<newOrder.length; i++) {
            sm.columnConfiguration[i] = (char)(newOrder[i] + 65);
        }
        //Generate the new column and row pointer arrays.
        int[] newColumnIndex = new int[this.elements];
        int[] newRowPointer = new int[this.rows + 1];

        // go throught the row pointer array and put the number of elements per row
        for (int i = 0; i < this.rows; i++) {
            int newRowIndex = newOrder[i];
            int rowLength = rowPointer[i + 1] - rowPointer[i];
            newRowPointer[newRowIndex + 1] = rowLength;
        }
        //Do another pass to put the exact start and end of each row in the new row pointer array
        for (int i = 1; i < newRowPointer.length; i++) {
            newRowPointer[i] += newRowPointer[i - 1];
        }

        //Now that we have the row pointers, we can add the columns in the new order.
        for (int i = 0; i < this.rows; i++) {
            int newRowIndex = newOrder[i];
            for (int j = rowPointer[i]; j < rowPointer[i + 1]; j++) {
                int col = columnIndex[j];
                int newColIndex = newOrder[col];
                newColumnIndex[newRowPointer[newRowIndex] + (j - rowPointer[i])] = newColIndex;
            }
        }

        sm.columnIndex = newColumnIndex;
        sm.rowPointer = newRowPointer;
        return sm;
    }

    /**
     * Calculate the loss function for a matrix
     */
    public long calculateLoss() {
        // The loss function is num_feedback_loops * num_rows^2 + sum_distance_from_diagonal
        // Using this we can support matrices up to the max int number of rows with sufficient penalty for adding additional feedback loops.
        long loss = 0;
        // Calculate the number of feedback loops and the distance from the diagonal for each element.
        for (int row = 0; row < this.rows; row++) {
            for (int j = rowPointer[row]; j < rowPointer[row + 1]; j++) {
                int col = columnIndex[j];
                if (col > row) {
                    loss += (long) this.rows * this.rows;
                    loss += (col - row);
                }
            }
        }
        return loss;
    }



        class Node {
        int index;
        boolean visited;

        public Node(int index) {
            this.index = index;
            this.visited = false;
        }
    }

    /** print a function that can be copied into Racket to show the matrix
     *  this probably wont be in the final version but it might
     */
    public void printRacketFunction() {
        System.out.print("(matrix-show \n\"");
        for (char c : columnConfiguration) System.out.print(c);
        System.out.println("\"");

        // print each line with dots
        // first make boolean array
        boolean[][] toPrint = new boolean[rows][columns];
        int j = 0; // the index of the row pointer
        for (int i=0; i<elements; i++) {
            if (rowPointer[j+1] <= i) j++;
            toPrint[j][columnIndex[i]] = true;
        }

        for (boolean[] i : toPrint) {
            System.out.print("\"");
            for (boolean k : i) {
                System.out.print(k ? "." : " ");
            }
            System.out.println("\"");
        }
        System.out.println(")");
    }
    
    
}
