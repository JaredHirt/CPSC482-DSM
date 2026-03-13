/**
 * A class representing a sparse matrix, which is a matrix that contains mostly zero values.
 * This class stores a binary Sparse matrix (values are either 0 or 1) in CRS format.
 * 
 */
// package DSM;
public class SparseMatrix {
    private int[] rowPointer;
    private int[] columnIndex;


    private SparseMatrix(int rows, int columns, int elements) {
        columnIndex = new int[elements];
        rowPointer = new int[rows + 1];
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
        // so we can operate on the basis of lines
        int lineIndex = 0;
        SparseMatrix sm;
        try (BufferedReader br = new BufferedReader(fr)) {

            // get past all the comments
            String line = "";
            boolean flag = false;

            // skip all commented lines
            while (flag == false){
                line = br.readLine().trim();
                System.out.println("on: " + line);
                if (!line.matches(".*%.*")) flag = true;
            }
            System.out.println("got here");

            // should be on size line now
            // read size and elements
            String[] data = line.split(" ");
            int row = Integer.parseInt(data[0]);
            int col = Integer.parseInt(data[1]);
            int ele = Integer.parseInt(data[2]);
            sm = new SparseMatrix(row, col, ele);

            System.out.println("got here");

            // start reading elements in
            int[][] allElements = new int[ele][2];
            int allElementIndex = 0;
            line = br.readLine().trim();
            // should be on first line of elements now
            while (line != null) {
                if (line == null) break;
                if (line.matches("%.*")) {line = br.readLine(); continue;}
                data = line.split(" ");
                allElements[allElementIndex++] = new int[]{
                    Integer.parseInt(data[0]), 
                    Integer.parseInt(data[1])
                };
                System.out.printf("{%d, %d}%n", Integer.parseInt(data[0]), Integer.parseInt(data[1]));
                line = br.readLine();
                if (line != null) line = line.trim();
            }
            // TODO: change the sorting method if Dr. Hossain doesnt like us doing this
            // sort to ease the process of putting in row-major form
            Arrays.sort(allElements, Comparator.comparing(x -> x[0]));

            // add to matrix
            int i = 0;
            int j = 0;
            for (int[] element : allElements){
                System.out.printf("{%d, %d}%n", element[0], element[1]);
                // subtract one from both coordinates so it's 0-indexed internally
                sm.columnIndex[i] = element[1] - 1;
                // go to next row if row number is bigger than the last
                // this for loop only executes if element[0] > j
                for (; j<element[0]; j++)
                    sm.rowPointer[j] = i;
                i++;
            }
            sm.rowPointer[row] = ele;
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
        return this;
    }
}
