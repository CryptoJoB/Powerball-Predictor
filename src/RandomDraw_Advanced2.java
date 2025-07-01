/*Generate 7 regular numbers + 1 powerball number lottery draw 
 * Updated to generate random draws based on pattern frequency
 */

import java.io.IOException;
import java.util.*;  

public class RandomDraw_Advanced2 {
    public static void main (String[] args) throws IOException {
        for (int i = 0; i < 8; i++) {
            System.out.println("Draw " + (i + 1) + ": " + CalculateNumbers(i).toString());
        }
    }

    public static ArrayList<Integer> CalculateNumbers(int drawNumber) {
        Random randomGen = new Random();
        ArrayList<Integer> numbers = new ArrayList<>();
        int random;
        int oddCount = 0;
        int evenCount = 0;

        // Set the number of odd and even numbers per draw
        int[][] drawConfigurations = {
            {4, 3},  // Draw 1: 4 odd, 3 even
            {3, 4},  // Draw 2: 4 odd, 3 even
            {2, 5},  // Draw 3: 2 odd, 5 even
            {5, 2},  // Draw 4: 5 odd, 2 even
            {1, 6},  // Draw 5: 1 odd, 6 even
            {6, 1},  // Draw 6: 6 odd, 1 even
            {0, 7},  // Draw 7: 0 odd, 7 even
            {7, 0}   // Draw 8: 7 odd, 0 even
        };

        int targetOdd = drawConfigurations[drawNumber][0];
        int targetEven = drawConfigurations[drawNumber][1];

        // Calculate 7 numbers from 1-35
        while (numbers.size() < 7) {
            random = randomGen.nextInt(35) + 1;
            if (!numbers.contains(random)) {
                if (random % 2 == 0 && evenCount < targetEven) {
                    numbers.add(random);
                    evenCount++;
                } else if (random % 2 != 0 && oddCount < targetOdd) {
                    numbers.add(random);
                    oddCount++;
                }
            }
        }

        // Calculate Powerball (random number from 1-20)
        random = randomGen.nextInt(20) + 1;
        numbers.add(random);

        return numbers;
    }
}