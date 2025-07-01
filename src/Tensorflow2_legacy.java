/*Origional ChatGPT code to use neural network model 
to "predict" (very unpredictable and random) lottery numbers

RUN IN CORRECT DIRECTORY AND IN TERMINAL - node Tensorflow2.java 
Must use subset data (only 7 + 1 numbers) and powerball stuck on max value (20)
*/

var tf = require('@tensorflow/tfjs');
var fs = require('fs');
var csv = require('csv-parser');  // Library to parse CSV files

// Constants for normalization
var MAX_NUMBER = 35;  // For normal numbers
var MAX_POWERBALL = 20;  // For Powerball number

// Sample function to normalize lotto numbers
function normalizeNumber(num, max) {
    return num / max;
}

// Function to load and process the CSV data
async function loadCSVData(filePath) {
    return new Promise((resolve, reject) => {
        var results = [];
        fs.createReadStream(filePath)
            .pipe(csv())
            .on('data', (row) => {
               //removed check for draw number
                    var numbers = Object.values(row).slice(0, 7).map(Number);  // First 8 values are numbers
                    var powerball = Object.values(row).slice(7, 8).map(Number);  // Extract the Powerball number

                    // Push the row data
                    results.push([numbers, powerball]);
                
            })
            .on('end', () => {
                resolve(results);  // Return the parsed data as a promise
            })
            .on('error', (error) => {
                reject(error);
            });
    });
}

// Prepare input-output pairs for the model
function prepareData(dataset) {
    var inputs = [];
    var labels = [];

    dataset.forEach((draw, index) => {
        if (index < dataset.length - 1) {
            // Normalize numbers between 0 and 1
            var input = draw[0].map(num => normalizeNumber(num, MAX_NUMBER));
            var powerball = normalizeNumber(draw[1], MAX_POWERBALL);

            inputs.push([...input, powerball]); // Combine drawn numbers and powerball into input

            // Output: next draw's 7 numbers + powerball
            var nextDraw = dataset[index + 1];
            var output = nextDraw[0].map(num => normalizeNumber(num, MAX_NUMBER));
            var nextPowerball = normalizeNumber(nextDraw[1], MAX_POWERBALL);

            labels.push([...output, nextPowerball]); // Combine next draw's numbers + powerball as labels
        }
    });

    return {
        xs: tf.tensor2d(inputs),
        ys: tf.tensor2d(labels)
    };
}

// Create the neural network model
function createModel() {
    var model = tf.sequential();

    // Input layer (8 values: 7 drawn numbers and 1 powerball)
    model.add(tf.layers.dense({ units: 128, inputShape: [8], activation: 'relu' })); 
    
    // Hidden layers
    // First hidden layer
    model.add(tf.layers.dense({ units: 256, activation: 'relu' }));
    model.add(tf.layers.batchNormalization());  // Batch normalization to stabilize training
    model.add(tf.layers.dropout({ rate: 0.3 }));  // Dropout to reduce overfitting

    // Second hidden layer
    model.add(tf.layers.dense({ units: 128, activation: 'relu' }));
    model.add(tf.layers.batchNormalization());
    model.add(tf.layers.dropout({ rate: 0.3 }));

    // Third hidden layer
    model.add(tf.layers.dense({ units: 64, activation: 'relu' }));
    model.add(tf.layers.batchNormalization());
    model.add(tf.layers.dropout({ rate: 0.3 }));

    // Fourth hidden layer (optional: experiment with deeper networks)
    model.add(tf.layers.dense({ units: 32, activation: 'relu' }));


    // Output layer (predict 8 values: 7 drawn numbers and 1 powerball)
    model.add(tf.layers.dense({ units: 8, activation: 'sigmoid' }));  // Sigmoid to output probabilities

    // Compile the model
    model.compile({
        optimizer: 'adam',
        loss: 'meanSquaredError',
        metrics: ['accuracy']
    });

    return model;
}

// Train the model
async function trainModel(model, xs, ys) {
    var history = await model.fit(xs, ys, {
        epochs: 50,    // Adjust number of epochs based on training results
        batchSize: 32, // Batch size
        validationSplit: 0.1,  // Reserve 10% of data for validation (Default 20%)
        callbacks: tf.callbacks.earlyStopping({ patience: 5 })  // Stop early if no improvement
    });

    console.log('Training complete.');
    return history;
}

// Predict the next draw numbers
async function predictNextDraw(model, lastDraw) {
    var input = lastDraw.map(num => normalizeNumber(num, MAX_NUMBER));
    var powerball = normalizeNumber(lastDraw[7], MAX_POWERBALL);

    var inputTensor = tf.tensor2d([input, powerball]);

    var prediction = model.predict(inputTensor);
    var output = prediction.arraySync()[0];

    // Convert the normalized predictions back to actual numbers
    var predictedNumbers = output.slice(0, 7).map(num => Math.round(num * MAX_NUMBER));
    var predictedPowerball = Math.round(output[7] * MAX_POWERBALL);

    console.log("Predicted numbers: ", predictedNumbers);
    console.log("Predicted Powerball: ", predictedPowerball);

    return {
        numbers: predictedNumbers,
        powerball: predictedPowerball
    };
}

// Main function to run everything
async function main() {
    var filePath = './powerball_results_subset1.csv';  // Path to your CSV file - Using subset (from 2018-2024 where 7 numbers 1 powerball)
    var dataset = await loadCSVData(filePath);  // Load data from CSV

    if (dataset.length === 0) {
        console.error('No data found or invalid file format.');
        return;
    }

    // Prepare the data
    var { xs, ys } = prepareData(dataset);
    
    var model = createModel();

    // Train the model
    await trainModel(model, xs, ys);

    // Predict the next lotto numbers using the last draw from your dataset
    var lastDraw = dataset[dataset.length - 1];
    await predictNextDraw(model, lastDraw[0].concat(lastDraw[1]));
}

// Run the main function
main();