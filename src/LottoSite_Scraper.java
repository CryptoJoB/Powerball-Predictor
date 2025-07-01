/*Scrape Aus historical Lottery data*/
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class LottoSite_Scraper {
    public static void main(String[] args) {
        String baseUrl = "https://australia.national-lottery.com/powerball/results-archive-"; // Base URL for each year
        int startYear = 2024;
        int endYear = 1996;
        String csvFile = "powerball_results.csv"; // Output CSV file

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Write CSV header
            writer.println("Numbers,Label");

            for (int year = startYear; year >= endYear; year--) {
                String url = baseUrl + year;
                System.out.println("Scraping year: " + year);

                try {
                    // Connect to the URL and parse the document
                    Document doc = Jsoup.connect(url).get();

                    // Select all <tr> elements, as each record is separated by <tr></tr>
                    Elements tableRows = doc.select("tr");

                    // Loop through each <tr> element to extract the list values and labels
                    for (Element row : tableRows) {
                        // Extract <li class="">x</li> elements within the row
                        Elements listItems = row.select("li");

                        // Extract <a href="" title="y"> elements within the row
                        Elements links = row.select("a[title]");

                        // Prepare to accumulate all list values in a single row
                        StringBuilder listValues = new StringBuilder();

                        // Loop through each list item and append the values
                        for (Element listItem : listItems) {
                            if (listValues.length() > 0) {
                                listValues.append(","); // Append comma for separation
                            }
                            listValues.append(listItem.text()); // Append the number
                        }

                        // Extract the label (e.g., draw number)
                        String label = "";
                        if (!links.isEmpty()) {
                            label = links.first().attr("title"); // Get the first <a> element's title
                        }

                        // Write the accumulated list values and label to the CSV
                        if (listValues.length() > 0) {
                            writer.println(listValues.toString() + "," + label);
                        }
                    }

                    // Flush the writer to ensure data is saved incrementally
                    writer.flush();

                } catch (IOException e) {
                    System.err.println("Error scraping year " + year + ": " + e.getMessage());
                }

                // Wait for 5 seconds before scraping the next year
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Sleep interrupted: " + e.getMessage());
                }
            }

            System.out.println("Scraping completed. Results saved to " + csvFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}