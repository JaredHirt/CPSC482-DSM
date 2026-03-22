package DSM;

class Main {
    public static void main(String[] args) {
        //Read and create initial sparse matrix
        //Get the transpose of the matrix
        //Perform DFS on the original matrix putting the equivalent nodes of the transposed matrix into a stack
        //Perform DFS on the transposed matrix, popping nodes from the stack and notating SCCs
        //Put the SCCs into order and rearange the original matrix accordingly
        //This will result with the DSM being optimized for the number of feed back loops


        //Work now needs to be performed on reducing the distance of feedback loops from the main diagonal. 
        //The perfect solution for this is exponential, but we will perform an approximate solution by using Neural sort.
        //Neural sort is a differentiable sorting algorithm that can be used to optimize the order of elements in a list.
        //This is done by relaxing the constraint of elements being in a specific order and instead allowing them to be in a continuous space.
        //This allows us to use gradient descent to optimize the order of elements in the list.
        //This is done relative to a loss function that we define. This will be (large number * num_feedback_loops) + distance_from_diagonal
    }
}