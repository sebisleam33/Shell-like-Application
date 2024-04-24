import java.util.*;

public class FarmOptimizer {
    static class Seed {
        char id;
        int cost;
        int profit;
        int bAdjEmpty;
        int bAdjObstacle;
        Map<Character, Integer> bAdjSeed = new HashMap<>();

        Seed(char id, int cost, int profit, int bAdjEmpty, int bAdjObstacle) {
            this.id = id;
            this.cost = cost;
            this.profit = profit;
            this.bAdjEmpty = bAdjEmpty;
            this.bAdjObstacle = bAdjObstacle;
        }
    }

    public static void main(String[] args) {
        int gx = 4; // Example grid bounds on X axis
        int gy = 4; // Example grid bounds on Y axis
        int ns = 2; // Example number of seed types
        char[][] farmGrid = {
            {'.', '.', '.', '#'},
            {'.', '#', '.', '.'},
            {'#', '.', '.', '.'},
            {'.', '.', '.', '.'}
        }; // Example farm grid
        
        Map<Character, Seed> seeds = new HashMap<>();
        seeds.put('a', new Seed('a', 1, 5, 2, 1)); // Example seed type a
        seeds.put('b', new Seed('b', 2, 7, 3, 2)); // Example seed type b

        char[][] optimalSeeds = optimizeFarm(gx, gy, ns, farmGrid, seeds);
        // Print optimal seed placement
        for (char[] row : optimalSeeds) {
            System.out.println(Arrays.toString(row));
        }
    }

    public static char[][] optimizeFarm(int gx, int gy, int ns, char[][] farmGrid, Map<Character, Seed> seeds) {
        int[][][] dp = new int[gx][gy][ns + 1]; // dp[i][j][k] represents the maximum profit at position (i, j) using the first k seed types
        char[][] optimalSeeds = new char[gx][gy];

        // Initialize dp array
        for (int[][] layer : dp) {
            for (int[] row : layer) {
                Arrays.fill(row, Integer.MIN_VALUE);
            }
        }
        dp[0][0][0] = 0; // Starting position with no seeds

        // Iterate through each cell in the farm grid
        for (int x = 0; x < gx; x++) {
            for (int y = 0; y < gy; y++) {
                // Iterate through each seed type
                for (int k = 0; k <= ns; k++) {
                    // If current cell is obstacle, skip
                    if (farmGrid[x][y] == '#') continue;

                    // Update dp array for each seed type
                    for (char seedId : seeds.keySet()) {
                        Seed seed = seeds.get(seedId);
                        int profit = seed.profit;
                        if (k > 0) {
                            profit -= seed.cost; // Subtract cost if planting a new seed
                            profit += seed.bAdjEmpty * countAdjacentEmpty(x, y, gx, gy, farmGrid);
                            profit += seed.bAdjObstacle * countAdjacentObstacle(x, y, gx, gy, farmGrid);
                            for (char prevSeedId : seeds.keySet()) {
                                profit += seed.bAdjSeed.getOrDefault(prevSeedId, 0) * countAdjacentSeed(x, y, gx, gy, farmGrid, prevSeedId);
                            }
                        }
                        dp[x][y][k] = Math.max(dp[x][y][k], profit);

                        // Update dp array based on adjacent cells
                        if (x > 0) dp[x][y][k] = Math.max(dp[x][y][k], dp[x - 1][y][k]);
                        if (y > 0) dp[x][y][k] = Math.max(dp[x][y][k], dp[x][y - 1][k]);
                        if (k > 0) dp[x][y][k] = Math.max(dp[x][y][k], dp[x][y][k - 1]);
                    }
                }
            }
        }

        // Find the cell with the highest profit
        int maxX = 0, maxY = 0, maxProfit = Integer.MIN_VALUE;
        for (int x = 0; x < gx; x++) {
            for (int y = 0; y < gy; y++) {
                if (dp[x][y][ns] > maxProfit) {
                    maxProfit = dp[x][y][ns];
                    maxX = x;
                    maxY = y;
                }
            }
        }

        // Backtrack from the cell with the highest profit to determine optimal seed placement
        int k = ns;
        while (maxX >= 0 && maxY >= 0 && k >= 0) {
            int profit = dp[maxX][maxY][k];
            for (char seedId : seeds.keySet()) {
                Seed seed = seeds.get(seedId);
                int curProfit = seed.profit;
                if (k > 0) {
                    curProfit -= seed.cost;
                    curProfit += seed.bAdjEmpty * countAdjacentEmpty(maxX, maxY, gx, gy, farmGrid);
                    curProfit += seed.bAdjObstacle * countAdjacentObstacle(maxX, maxY, gx, gy, farmGrid);
                    for (char prevSeedId : seeds.keySet()) {
                        curProfit += seed.bAdjSeed.getOrDefault(prevSeedId, 0) * countAdjacentSeed(maxX, maxY, gx, gy, farmGrid, prevSeedId);
                    }
                }
                if (profit == curProfit) {
                    optimalSeeds[maxX][maxY] = seedId;
                    maxX--;
                    if (maxX < 0) {
                        maxX = gx - 1;
                        maxY--;
                    }
                    break;
                }
            }
            k--;
        }

        return optimalSeeds;
    }

    // Function to count adjacent empty cells
    private static int countAdjacentEmpty(int x, int y, int gx, int gy, char[][] farmGrid) {
        int count = 0;
        if (x > 0 && farmGrid[x - 1][y] == '.') count++;
        if (x < gx - 1 && farmGrid[x + 1][y] == '.') count++;
        if (y > 0 && farmGrid[x][y - 1] == '.') count++;
        if (y < gy - 1 && farmGrid[x][y + 1] == '.') count++;
        return count;
    }

    // Function to count adjacent obstacle cells
    private static int countAdjacentObstacle(int x, int y, int gx, int gy, char[][] farmGrid) {
        int count = 0;
        if (x > 0 && farmGrid[x - 1][y] == '#') count++;
        if (x < gx - 1 && farmGrid[x + 1][y] == '#') count++;
        if (y > 0 && farmGrid[x][y - 1] == '#') count++;
        if (y < gy - 1 && farmGrid[x][y + 1] == '#') count++;
        return count;
    }

    // Function to count adjacent cells with a specific seed type
    private static int countAdjacentSeed(int x, int y, int gx, int gy, char[][] farmGrid, char seedId) {
        int count = 0;
        if (x > 0 && farmGrid[x - 1][y] == seedId) count++;
        if (x < gx - 1 && farmGrid[x + 1][y] == seedId) count++;
        if (y > 0 && farmGrid[x][y - 1] == seedId) count++;
        if (y < gy - 1 && farmGrid[x][y + 1] == seedId) count++;
        return count;
    }
}
