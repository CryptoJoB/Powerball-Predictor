/*Calculates pattern frequnecy of Powerball numbers
 * -How many even/odd numbers in a draw
 * run using java Frequency_Analyzer1.java
 * 
 * More info - https://lottometrix.com/blog/how-to-win-the-lottery-according-to-math/
 */
import java.io.*;
import java.util.*;

public class Lotto_Historical_Freq1 {

    public static void main(String[] args) {
        String csvFile = "./powerball_results_subset.csv"; // Ensure correct file path
        String line = "";
        String csvSplitBy = ",";

        // Map to store pattern frequency
        Map<String, Integer> patternFrequency = new HashMap<>();
        final int[] totalDraws = {0};  // Use an array to make totalDraws mutable inside lambda

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {
                // Split the CSV line by commas
                String[] draw = line.split(csvSplitBy);

                // Ensure there are at least 8 numbers (7 regular numbers + 1 Powerball)
                if (draw.length < 8) {
                    System.out.println("Error: Insufficient numbers in draw. Skipping this draw.");
                    continue;  // Skip any invalid rows
                }

                int[] numbers = new int[8];  // Array to store 7 numbers + Powerball
                boolean validNumbers = true;

                // Parse the 7 regular numbers and the 1 Powerball number (ignoring non-number text)
                for (int i = 0; i < 8; i++) {
                    try {
                        numbers[i] = Integer.parseInt(draw[i].trim()); // Convert to integer
                    } catch (NumberFormatException e) {
                        validNumbers = false;
                        System.out.println("Invalid number found in draw. Skipping this draw: " + Arrays.toString(draw));
                        break;
                    }
                }

                // If valid numbers were parsed, process the draw
                if (validNumbers) {
                    // Calculate the odd/even pattern for this draw
                    String pattern = calculateOddEvenPattern(numbers);

                    // Update pattern frequency map
                    patternFrequency.put(pattern, patternFrequency.getOrDefault(pattern, 0) + 1);
                    totalDraws[0]++;  // Increment the draw count
                }
            }

            // Display the results sorted by most frequent patterns
            patternFrequency.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))  // Sort by frequency (descending)
                .forEach(entry -> {
                    double probability = (double) entry.getValue() / totalDraws[0] * 100;  // Convert to percentage
                    System.out.println("Pattern: " + entry.getKey() + " - " + entry.getValue() + " times (" 
                                       + String.format("%.2f", probability) + "%)");
                });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to calculate the odd/even pattern for a draw (including Powerball)
    public static String calculateOddEvenPattern(int[] numbers) {
        int oddCount = 0;
        int evenCount = 0;

        // Loop through all 8 numbers (7 regular numbers + 1 Powerball)
        for (int number : numbers) {
            if (number % 2 == 0) {
                evenCount++;
            } else {
                oddCount++;
            }
        }

        // Return the pattern as a string (e.g., "4 odd + 4 even")
        return oddCount + " odd + " + evenCount + " even";
    }
}