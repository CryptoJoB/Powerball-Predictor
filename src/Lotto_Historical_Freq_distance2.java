import java.io.*;
import java.util.*;

public class Lotto_Historical_Freq_distance2 {

    public static void main(String[] args) {
        String csvFile = "./powerball_results_subset.csv"; // Ensure correct file path
        String line = "";
        String csvSplitBy = ",";

        // Variables to calculate overall average distance
        double totalDistanceSum = 0;
        int totalValidDraws = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {
                // Split the CSV line by commas
                String[] draw = line.split(csvSplitBy);

                // Ensure there are at least 8 numbers (7 regular numbers + 1 Powerball)
                if (draw.length < 8) {
                    System.out.println("Error: Insufficient numbers in draw. Skipping this draw.");
                    continue; // Skip any invalid rows
                }

                int[] numbers = new int[8]; // Array to store 7 numbers + Powerball
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
                    // Sort the numbers to calculate distances
                    Arrays.sort(numbers);

                    // Calculate the average distance for this draw
                    double averageDistance = calculateAverageDistance(numbers);

                    // Add to total distance sum and increment valid draw count
                    totalDistanceSum += averageDistance;
                    totalValidDraws++;
                }
            }

            // Calculate and display the overall average distance
            if (totalValidDraws > 0) {
                double overallAverageDistance = totalDistanceSum / totalValidDraws;
                System.out.println("Overall Average Distance: " + String.format("%.2f", overallAverageDistance));
            } else {
                System.out.println("No valid draws to calculate average distance.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to calculate the average distance between sorted numbers in a draw
    public static double calculateAverageDistance(int[] numbers) {
        int totalDistance = 0;
        int count = numbers.length - 1; // Number of distances between consecutive numbers

        // Calculate distances between consecutive numbers
        for (int i = 0; i < count; i++) {
            totalDistance += Math.abs(numbers[i + 1] - numbers[i]);
        }

        // Return the average distance for this draw
        return (double) totalDistance / count;
    }
}
