package DSM;

import java.io.IOException;

class Test {

    private static class TestCase {
        String name;
        String filePath;
        String description;

        TestCase(String name, String filePath, String description) {
            this.name = name;
            this.filePath = filePath;
            this.description = description;
        }
    }

    public static void main(String[] args) {
        TestCase[] tests = new TestCase[] {
            new TestCase(
                "Empty Matrix",
                "TestMatrices/Empty4.mtx",
                "Edge case with zero edges. Verifies CSR read/store with no elements, isomorphism after optimize, and global minimum feedback loops of 0."
            ),
            new TestCase(
                "Simple DAG Chain",
                "TestMatrices/Chain4.mtx",
                "Acyclic graph (1->2->3->4). Verifies optimization keeps graph isomorphic and reaches global minimum feedback loops (0 for DAG)."
            ),
            new TestCase(
                "Single SCC Cycle",
                "TestMatrices/Cycle3.mtx",
                "Single strongly connected cycle of size 3. Verifies SCC handling and that optimized order attains globally minimal unavoidable feedback loops."
            ),
            new TestCase(
                "Two SCCs With Bridge",
                "TestMatrices/TwoSCC4.mtx",
                "Two separate SCCs linked by one cross edge. Verifies SCC condensation ordering and global minimum feedback loops across all permutations."
            ),
            new TestCase(
                "Trailing Empty Rows",
                "TestMatrices/SparseTrailingRows6.mtx",
                "Matrix with edges only in early rows and empty rows at the end. Verifies CSR rowPointer correctness, isomorphism, and optimal feedback loop count."
            ),
            new TestCase(
                "Self Loops And Sparse Edges",
                "TestMatrices/SelfLoops5.mtx",
                "Includes diagonal self-loops and sparse forward edges. Verifies that optimization preserves structure and still minimizes feedback loops globally."
            )
        };

        int passed = 0;
        int failed = 0;
        for (TestCase testCase : tests) {
            try {
                runTest(testCase);
                passed++;
            } catch (AssertionError e) {
                failed++;
                System.out.println("Result: FAIL");
                System.out.println("Reason: " + e.getMessage());
            } catch (Exception e) {
                failed++;
                System.out.println("Result: FAIL");
                System.out.println("Reason: Unexpected exception - " + e);
            }
        }

        System.out.println();
        System.out.println("Summary: " + passed + " passed, " + failed + " failed, " + tests.length + " total");
        if (failed > 0) {
            throw new AssertionError("One or more tests failed.");
        }
    }

    private static void runTest(TestCase testCase) throws IOException {
        System.out.println("----------------------------------------");
        System.out.println("Test: " + testCase.name);
        System.out.println("Description: " + testCase.description);

        SparseMatrix original = SparseMatrix.readFile(testCase.filePath);
        SparseMatrix optimized = SparseMatrix.readFile(testCase.filePath);
        optimized.optimizeDSM();

        boolean[][] originalAdj = toAdjacencyMatrix(original);
        boolean[][] optimizedAdj = toAdjacencyMatrix(optimized);

        boolean isomorphic = areIsomorphic(originalAdj, optimizedAdj);
        if (!isomorphic) {
            System.out.println("Original edges: " + edgeListString(originalAdj));
            System.out.println("Optimized edges: " + edgeListString(optimizedAdj));
        }
        assertCondition(isomorphic, "Result graph is not isomorphic to the input graph");

        int optimizedFeedbackLoops = countFeedbackLoops(optimizedAdj);
        int globalMinimumFeedbackLoops = computeGlobalMinimumFeedbackLoops(originalAdj);

        assertCondition(
            optimizedFeedbackLoops == globalMinimumFeedbackLoops,
            "Feedback loops not globally minimized. Expected minimum "
                + globalMinimumFeedbackLoops
                + " but got "
                + optimizedFeedbackLoops
        );

        System.out.println("Result: PASS");
        System.out.println("Optimized feedback loops: " + optimizedFeedbackLoops);
        System.out.println("Global minimum feedback loops: " + globalMinimumFeedbackLoops);
    }

    private static boolean[][] toAdjacencyMatrix(SparseMatrix matrix) {
        int[] rowPointer = matrix.getRowPointer();
        int[] columnIndex = matrix.getColumnIndex();
        int n = rowPointer.length - 1;

        boolean[][] adj = new boolean[n][n];
        for (int row = 0; row < n; row++) {
            for (int i = rowPointer[row]; i < rowPointer[row + 1]; i++) {
                adj[row][columnIndex[i]] = true;
            }
        }
        return adj;
    }

    private static int countFeedbackLoops(boolean[][] adj) {
        int n = adj.length;
        int count = 0;

        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (adj[row][col] && col < row) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int computeGlobalMinimumFeedbackLoops(boolean[][] originalAdj) {
        int n = originalAdj.length;

        int[][] edges = extractEdges(originalAdj);
        int[] permutation = new int[n];
        boolean[] used = new boolean[n];
        int[] best = new int[] {Integer.MAX_VALUE};

        searchMinimumFeedbackPermutation(0, permutation, used, edges, best);
        return best[0];
    }

    private static void searchMinimumFeedbackPermutation(
        int depth,
        int[] permutation,
        boolean[] used,
        int[][] edges,
        int[] best
    ) {
        int n = permutation.length;
        if (depth == n) {
            int feedbackCount = 0;
            for (int[] edge : edges) {
                int source = edge[0];
                int target = edge[1];
                if (permutation[target] < permutation[source]) {
                    feedbackCount++;
                }
            }
            if (feedbackCount < best[0]) {
                best[0] = feedbackCount;
            }
            return;
        }

        for (int candidate = 0; candidate < n; candidate++) {
            if (used[candidate]) {
                continue;
            }
            permutation[depth] = candidate;
            used[candidate] = true;
            searchMinimumFeedbackPermutation(depth + 1, permutation, used, edges, best);
            used[candidate] = false;
        }
    }

    private static boolean areIsomorphic(boolean[][] originalAdj, boolean[][] resultAdj) {
        int n = originalAdj.length;
        if (resultAdj.length != n) {
            return false;
        }

        int[] inDegreeOriginal = computeInDegree(originalAdj);
        int[] outDegreeOriginal = computeOutDegree(originalAdj);
        int[] inDegreeResult = computeInDegree(resultAdj);
        int[] outDegreeResult = computeOutDegree(resultAdj);

        int[] permutation = new int[n];
        boolean[] used = new boolean[n];

        return searchIsomorphism(
            0,
            permutation,
            used,
            originalAdj,
            resultAdj,
            inDegreeOriginal,
            outDegreeOriginal,
            inDegreeResult,
            outDegreeResult
        );
    }

    private static boolean searchIsomorphism(
        int depth,
        int[] permutation,
        boolean[] used,
        boolean[][] originalAdj,
        boolean[][] resultAdj,
        int[] inDegreeOriginal,
        int[] outDegreeOriginal,
        int[] inDegreeResult,
        int[] outDegreeResult
    ) {
        int n = permutation.length;
        if (depth == n) {
            return checkPermutationIsomorphism(permutation, originalAdj, resultAdj);
        }

        for (int candidate = 0; candidate < n; candidate++) {
            if (used[candidate]) {
                continue;
            }
            if (inDegreeOriginal[depth] != inDegreeResult[candidate]) {
                continue;
            }
            if (outDegreeOriginal[depth] != outDegreeResult[candidate]) {
                continue;
            }

            boolean consistent = true;
            for (int prev = 0; prev < depth; prev++) {
                int mappedPrev = permutation[prev];
                if (originalAdj[prev][depth] != resultAdj[mappedPrev][candidate]) {
                    consistent = false;
                    break;
                }
                if (originalAdj[depth][prev] != resultAdj[candidate][mappedPrev]) {
                    consistent = false;
                    break;
                }
            }
            if (!consistent) {
                continue;
            }

            permutation[depth] = candidate;
            used[candidate] = true;
            if (searchIsomorphism(
                depth + 1,
                permutation,
                used,
                originalAdj,
                resultAdj,
                inDegreeOriginal,
                outDegreeOriginal,
                inDegreeResult,
                outDegreeResult
            )) {
                return true;
            }
            used[candidate] = false;
        }

        return false;
    }

    private static boolean checkPermutationIsomorphism(int[] permutation, boolean[][] originalAdj, boolean[][] resultAdj) {
        int n = permutation.length;
        for (int source = 0; source < n; source++) {
            for (int target = 0; target < n; target++) {
                if (originalAdj[source][target] != resultAdj[permutation[source]][permutation[target]]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int[] computeInDegree(boolean[][] adj) {
        int n = adj.length;
        int[] inDegree = new int[n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (adj[row][col]) {
                    inDegree[col]++;
                }
            }
        }
        return inDegree;
    }

    private static int[] computeOutDegree(boolean[][] adj) {
        int n = adj.length;
        int[] outDegree = new int[n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (adj[row][col]) {
                    outDegree[row]++;
                }
            }
        }
        return outDegree;
    }

    private static int[][] extractEdges(boolean[][] adj) {
        int n = adj.length;
        int edgeCount = 0;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (adj[row][col]) {
                    edgeCount++;
                }
            }
        }

        int[][] edges = new int[edgeCount][2];
        int idx = 0;
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if (adj[row][col]) {
                    edges[idx++] = new int[] {row, col};
                }
            }
        }
        return edges;
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static String edgeListString(boolean[][] adj) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (int row = 0; row < adj.length; row++) {
            for (int col = 0; col < adj.length; col++) {
                if (adj[row][col]) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append("(").append(row).append("->").append(col).append(")");
                    first = false;
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
