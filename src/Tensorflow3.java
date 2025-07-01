/*Neural network model to "predict" (very unpredictable and random) 
lottery numbers

 RUN IN CORRECT DIRECTORY AND IN TERMINAL - node Tensorflowx.java
 Changes to legacy code 
    •	Normalization Functions:
	•	Original normalization kept, but now applied across both regular numbers and Powerball numbers with adjusted range.
    
	•	Mode Calculation:
	•	Added a function calculateMode to calculate the most frequent regular number from the dataset.
	•	This mode is then used to fill/pad draws with fewer than 7 regular numbers.

	•	Padding with Mode:
	•	Introduced padNumbersWithMode to pad the missing regular numbers using the mode, instead of random or average values.

	•	Dataset Handling:
	•	Modified the loadCSVData function to first load the dataset without padding, calculate the mode, and then pad rows with fewer than 7 numbers using the mode.

	•	Data Preparation:
	•	Adjusted prepareData to handle 7 regular numbers and 1 Powerball number, ensuring correct input and label generation for the model.

	•	Neural Network Model:
	•	Redesigned to use a shared input with two outputs:
	•	One for predicting 7 regular numbers.
	•	Another for predicting 1 Powerball number.
	•	Output branch for regular numbers predicts 7 values.
	•	Output branch for Powerball predicts 1 value.

	•	Loss Function:
	•	Applied different loss functions for the regular numbers and Powerball prediction.

	•	Error Handling:
	•	Fixed shape mismatches and errors related to input/output dimensionality (expected input1 to have shape [null,8] but got array with shape [1,9]).

	•	Prediction Handling:
	•	Updated predictNextDraw function to ensure it predicts exactly 7 regular numbers and 1 Powerball, handling the output size and normalization properly. */


var tf = require('@tensorflow/tfjs');
var fs = require('fs');
var csv = require('csv-parser');

// Constants for normalization
const MAX_NUMBER = 35;  // For regular numbers
const MAX_POWERBALL = 20;  // For Powerball number
const REQUIRED_REGULAR_NUMBERS = 7;  // Always predict 7 regular numbers

// Function to normalize lotto numbers
function normalizeNumber(num, max) {
    return num / max;
}

// Helper function to calculate the mode of an array
function calculateMode(numbers) {
    const frequency = {};
    let maxFreq = 0;
    let mode = null;

    numbers.forEach((num) => {
        if (num in frequency) {
            frequency[num]++;
        } else {
            frequency[num] = 1;
        }

        if (frequency[num] > maxFreq) {
            maxFreq = frequency[num];
            mode = num;
        }
    });

    return mode;
}

// Function to pad numbers with mode if fewer than 7 numbers
function padNumbersWithMode(numbers, mode, requiredLength) {
    while (numbers.length < requiredLength) {
        numbers.push(mode);  // Pad with the mode
    }
    return numbers;
}

// Function to load and process the CSV data
async function loadCSVData(filePath) {
    return new Promise((resolve, reject) => {
        let results = [];
        let allRegularNumbers = [];  // Collect all regular numbers to calculate the mode later

        fs.createReadStream(filePath)
            .pipe(csv())
            .on('data', (row) => {
                // Extract numbers from the row, assuming the Powerball is the last number before non-numeric fields
                const numericValues = Object.values(row).filter(val => !isNaN(val) && val.trim() !== '');  // Extract only numeric values
                
                if (numericValues.length >= 6) {
                    let numbers = numericValues.slice(0, -1).map(Number);  // Regular numbers (5 to 7)
                    let powerball = Number(numericValues[numericValues.length - 1]);  // Powerball number

                    // Store the regular numbers for mode calculation
                    allRegularNumbers.push(...numbers);

                    // Push the row data without padding (we'll pad later after mode calculation)
                    results.push([numbers, powerball]);
                }
            })
            .on('end', () => {
                // Calculate mode of the regular numbers
                const mode = calculateMode(allRegularNumbers);

                // Pad all rows with fewer than 7 numbers using the calculated mode
                results = results.map(draw => {
                    draw[0] = padNumbersWithMode(draw[0], mode, REQUIRED_REGULAR_NUMBERS);
                    return draw;
                });

                resolve(results);  // Return the processed data
            })
            .on('error', (error) => {
                reject(error);
            });
    });
}

// Prepare input-output pairs for the model
function prepareData(dataset) {
    let inputs = [];
    let regularNumberLabels = [];
    let powerballLabels = [];

    dataset.forEach((draw, index) => {
        if (index < dataset.length - 1) {
            // Normalize the input: 7 regular numbers + powerball number
            const input = draw[0].map(num => normalizeNumber(num, MAX_NUMBER));
            const powerball = normalizeNumber(draw[1], MAX_POWERBALL);

            inputs.push([...input, powerball]);  // Input: [7 regular numbers + 1 powerball]

            // Prepare the labels (next draw):
            const nextDraw = dataset[index + 1];
            const regularNumbers = nextDraw[0].map(num => normalizeNumber(num, MAX_NUMBER));
            const nextPowerball = normalizeNumber(nextDraw[1], MAX_POWERBALL);

            regularNumberLabels.push(regularNumbers);  // Label for 7 regular numbers
            powerballLabels.push([nextPowerball]);  // Label for the powerball (shape [*, 1])
        }
    });

    return {
        xs: tf.tensor2d(inputs),
        regularNumberYs: tf.tensor2d(regularNumberLabels),
        powerballYs: tf.tensor2d(powerballLabels)
    };
}

// Create the neural network model with two output branches
function createModel() {
    const inputLayer = tf.input({ shape: [8] });  // Input is 7 regular numbers + 1 powerball

    // Shared input and hidden layers
    let sharedLayer = tf.layers.dense({ units: 128, activation: 'relu' }).apply(inputLayer);
    sharedLayer = tf.layers.dense({ units: 256, activation: 'relu' }).apply(sharedLayer);
    sharedLayer = tf.layers.batchNormalization().apply(sharedLayer);
    sharedLayer = tf.layers.dropout({ rate: 0.3 }).apply(sharedLayer);

    sharedLayer = tf.layers.dense({ units: 128, activation: 'relu' }).apply(sharedLayer);
    sharedLayer = tf.layers.batchNormalization().apply(sharedLayer);
    sharedLayer = tf.layers.dropout({ rate: 0.3 }).apply(sharedLayer);

    // Regular numbers output branch
    const regularNumbersOutput = tf.layers.dense({ units: 7, activation: 'sigmoid', name: 'regularNumbers' }).apply(sharedLayer);

    // Powerball output branch
    const powerballOutput = tf.layers.dense({ units: 1, activation: 'linear', name: 'powerball' }).apply(sharedLayer);

    // Create the model with two outputs
    const model = tf.model({
        inputs: inputLayer,
        outputs: [regularNumbersOutput, powerballOutput]
    });

    // Compile the model with separate loss functions for each output
    model.compile({
        optimizer: 'adam',
        loss: {
            regularNumbers: 'meanSquaredError',
            powerball: 'meanSquaredError'
        },
        metrics: ['accuracy']
    });

    return model;
}

// Train the model
async function trainModel(model, xs, regularNumberYs, powerballYs) {
    const history = await model.fit(xs, { regularNumbers: regularNumberYs, powerball: powerballYs }, {
        epochs: 50,    // Adjust number of epochs based on training results
        batchSize: 32, // Batch size
        validationSplit: 0.1,  // Reserve 10% of data for validation
        callbacks: tf.callbacks.earlyStopping({ patience: 5 })  // Stop early if no improvement
    });

    console.log('Training complete.');
    return history;
}

// Predict the next draw numbers
async function predictNextDraw(model, lastDraw) {
    // Ensure that you are passing exactly 7 regular numbers and 1 powerball (8 values in total)
    const input = lastDraw.slice(0, 7).map(num => normalizeNumber(num, MAX_NUMBER));  // Slice to take only 7 numbers
    const powerball = normalizeNumber(lastDraw[7], MAX_POWERBALL);  // Take only 1 powerball

    const inputTensor = tf.tensor2d([input.concat([powerball])]);  // Concatenate to ensure it's 8 values

    // Predict regular numbers and Powerball
    const [regularNumbersPrediction, powerballPrediction] = model.predict(inputTensor);

    // Extract the 7 predicted numbers and the Powerball
    let predictedNumbers = regularNumbersPrediction.arraySync()[0].map(num => Math.round(num * MAX_NUMBER));
    let predictedPowerball = Math.round(powerballPrediction.arraySync()[0][0] * MAX_POWERBALL);

    // Post-process to ensure no zero values (since lotto numbers start from 1)
    predictedNumbers = predictedNumbers.map(num => num < 1 ? 1 : num);

    console.log("Predicted numbers: ", predictedNumbers);
    console.log("Predicted Powerball: ", predictedPowerball);

    return {
        numbers: predictedNumbers,
        powerball: predictedPowerball
    };
}

// Main function to run everything
async function main() {
    const filePath = './powerball_results.csv';  // Path to your CSV file
    const dataset = await loadCSVData(filePath);  // Load data from CSV

    if (dataset.length === 0) {
        console.error('No data found or invalid file format.');
        return;
    }

    // Prepare the data
    const { xs, regularNumberYs, powerballYs } = prepareData(dataset);

    const model = createModel();

    // Train the model
    await trainModel(model, xs, regularNumberYs, powerballYs);

    // Predict the next lotto numbers using the last draw from your dataset
    const lastDraw = dataset[dataset.length - 1];
    await predictNextDraw(model, lastDraw[0].concat(lastDraw[1]));
}

// Run the main function
main();