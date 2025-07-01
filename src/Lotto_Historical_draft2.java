//Expanded version with different analysis options (have removed some)
//Number frequency analysis matches reported online lotto statistics

//run using java Frequency_Analyzer1.java

import java.io.*;
import java.util.*;

public class Lotto_Historical_draft2 {
    public static void main(String[] args) throws IOException {
        String filePath = "./powerball_results_subset_no_label.csv"; // Path to your CSV file
        List<int[]> draws = readPowerballDraws(filePath);  // Read the draws from the CSV file

        // Suggestion 1: Number Frequency Analysis
        int[] regularNumberFrequency = new int[35]; // For numbers 1-35
        int[] powerballFrequency = new int[20];     // For Powerball numbers 1-20
        numberFrequencyAnalysis(draws, regularNumberFrequency, powerballFrequency);

        // Suggestion 2: Range Analysis
        //rangeAnalysis(draws);

        // Suggestion 3: Sum of Numbers Analysis
        //sumOfNumbersAnalysis(draws);

        // Suggestion 4: Odd-Even Analysis for Powerball and Regular Numbers
        //oddEvenAnalysis(draws);

        // Suggestion 5: Repeating Numbers Across Draws
        //repeatingNumbersAnalysis(draws);

        // Suggestion 6: Consecutive Numbers Analysis
        //consecutiveNumbersAnalysis(draws);

        // Suggestion 7: Powerball-Only Analysis (included in odd-even and frequency analysis)

        // Suggestion 8: Most Common Combinations
        //mostCommonCombinations(draws);
    }

    // Function to read the Powerball draws from a CSV file
    public static List<int[]> readPowerballDraws(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        List<int[]> draws = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            int[] draw = new int[8];  // 7 regular numbers + 1 Powerball
            for (int i = 0; i < 8; i++) {
                draw[i] = Integer.parseInt(parts[i].trim());  // Parse each number
            }
            draws.add(draw);
        }
        reader.close();
        return draws;
    }

    // Suggestion 1: Number Frequency Analysis
    public static void numberFrequencyAnalysis(List<int[]> draws, int[] regularFreq, int[] powerballFreq) {
        for (int[] draw : draws) {
            for (int i = 0; i < 7; i++) {
                regularFreq[draw[i] - 1]++;
            }
            powerballFreq[draw[7] - 1]++;
        }

        System.out.println("Regular Number Frequency (1-35): ");
        for (int i = 0; i < regularFreq.length; i++) {
            System.out.println((i + 1) + ": " + regularFreq[i] + " times");
        }

        System.out.println("Powerball Number Frequency (1-20): ");
        for (int i = 0; i < powerballFreq.length; i++) {
            System.out.println((i + 1) + ": " + powerballFreq[i] + " times");
        }
    }

    // Suggestion 2: Range Analysis
    public static void rangeAnalysis(List<int[]> draws) {
        System.out.println("Range Analysis: ");
        for (int[] draw : draws) {
            int min = Arrays.stream(draw, 0, 7).min().getAsInt();
            int max = Arrays.stream(draw, 0, 7).max().getAsInt();
            int range = max - min;
            System.out.println("Range of draw: " + range);
        }
    }

    // Suggestion 3: Sum of Numbers Analysis
    public static void sumOfNumbersAnalysis(List<int[]> draws) {
        System.out.println("Sum of Numbers Analysis: ");
        for (int[] draw : draws) {
            int sum = Arrays.stream(draw, 0, 7).sum();
            System.out.println("Sum of draw: " + sum);
        }
    }

    // Suggestion 4: Odd-Even Analysis for Powerball and Regular Numbers
    public static void oddEvenAnalysis(List<int[]> draws) {
        System.out.println("Odd-Even Analysis: ");
        for (int[] draw : draws) {
            int oddCount = 0, evenCount = 0;
            for (int i = 0; i < 7; i++) {
                if (draw[i] % 2 == 0) {
                    evenCount++;
                } else {
                    oddCount++;
                }
            }
            // Also check the Powerball number
            if (draw[7] % 2 == 0) {
                evenCount++;
            } else {
                oddCount++;
            }
            System.out.println("Odd-Even pattern: " + oddCount + " odd, " + evenCount + " even");
        }
    }

    // Suggestion 5: Repeating Numbers Across Draws
    public static void repeatingNumbersAnalysis(List<int[]> draws) {
        Set<Integer> uniqueNumbers = new HashSet<>();
        System.out.println("Repeating Numbers Across Draws: ");
        for (int[] draw : draws) {
            for (int num : draw) {
                if (!uniqueNumbers.add(num)) {
                    System.out.println("Repeating number: " + num);
                }
            }
        }
    }

    // Suggestion 6: Consecutive Numbers Analysis
    public static void consecutiveNumbersAnalysis(List<int[]> draws) {
        System.out.println("Consecutive Numbers Analysis: ");
        for (int[] draw : draws) {
            int consecutiveCount = 0;
            for (int i = 0; i < 6; i++) {
                if (draw[i + 1] == draw[i] + 1) {
                    consecutiveCount++;
                }
            }
            System.out.println("Number of consecutive numbers in draw: " + consecutiveCount);
        }
    }

    // Suggestion 8: Most Common Combinations
    public static void mostCommonCombinations(List<int[]> draws) {
        Map<String, Integer> combinationFrequency = new HashMap<>();
        for (int[] draw : draws) {
            String combination = Arrays.toString(Arrays.copyOfRange(draw, 0, 7));
            combinationFrequency.put(combination, combinationFrequency.getOrDefault(combination, 0) + 1);
        }

        System.out.println("Most Common Combinations: ");
        combinationFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> System.out.println("Combination: " + entry.getKey() + " appeared " + entry.getValue() + " times"));
    }
}