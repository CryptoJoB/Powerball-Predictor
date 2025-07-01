/*Neural network model to "predict" (very unpredictable and random) 
lottery numbers

More advanced version of Tensorflow3 and more training 

 RUN IN CORRECT DIRECTORY AND IN TERMINAL - node Tensorflowx.java
 */

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

// Augment the data by slightly perturbing regular numbers
function augmentData(draw) {
    const augmented = draw[0].map(num => num + Math.random() - 0.5);  // Small random perturbation
    return [augmented, draw[1]];
}

// Prepare input-output pairs for the model with sequential data (time steps)
function prepareData(dataset, timeSteps = 10) {
    let inputs = [];
    let regularNumberLabels = [];
    let powerballLabels = [];

    for (let i = 0; i < dataset.length - timeSteps; i++) {
        // Prepare input sequences of 'timeSteps' length
        const inputSequence = [];

        for (let j = 0; j < timeSteps; j++) {
            const draw = dataset[i + j];
            const augmentedDraw = augmentData(draw);  // Apply data augmentation
            const input = augmentedDraw[0].map(num => normalizeNumber(num, MAX_NUMBER));
            const powerball = normalizeNumber(augmentedDraw[1], MAX_POWERBALL);
            inputSequence.push([...input, powerball]);  // Combine regular numbers and powerball
        }

        inputs.push(inputSequence);  // Each input now has shape [timeSteps, 8]

        // Prepare the labels (next draw):
        const nextDraw = dataset[i + timeSteps];
        const regularNumbers = nextDraw[0].map(num => normalizeNumber(num, MAX_NUMBER));
        const nextPowerball = normalizeNumber(nextDraw[1], MAX_POWERBALL);

        regularNumberLabels.push(regularNumbers);  // Label for 7 regular numbers
        powerballLabels.push([nextPowerball]);  // Label for the powerball (shape [*, 1])
    }

    return {
        xs: tf.tensor3d(inputs),  // 3D tensor for LSTM: [batch_size, time_steps, features]
        regularNumberYs: tf.tensor2d(regularNumberLabels),  // 2D tensor for regular numbers
        powerballYs: tf.tensor2d(powerballLabels)  // 2D tensor for powerball
    };
}

// Create the neural network model with two output branches
function createModel(timeSteps) {
    const inputLayer = tf.input({ shape: [timeSteps, 8] });  // Input is 7 regular numbers + 1 powerball

    // LSTM layer to process sequences of past draws
    let lstmLayer = tf.layers.lstm({ units: 64, returnSequences: false }).apply(inputLayer);

    // Shared dense layers after LSTM
    let sharedLayer = tf.layers.dense({ units: 128 }).apply(lstmLayer);
    sharedLayer = tf.layers.leakyReLU({ alpha: 0.01 }).apply(sharedLayer);  // Apply LeakyReLU activation

    sharedLayer = tf.layers.dense({ units: 256 }).apply(sharedLayer);
    sharedLayer = tf.layers.leakyReLU({ alpha: 0.01 }).apply(sharedLayer);  // Apply LeakyReLU activation

    sharedLayer = tf.layers.batchNormalization().apply(sharedLayer);
    sharedLayer = tf.layers.dropout({ rate: 0.4 }).apply(sharedLayer);  // Increased dropout rate for regularization

    sharedLayer = tf.layers.dense({ units: 128 }).apply(sharedLayer);
    sharedLayer = tf.layers.leakyReLU({ alpha: 0.01 }).apply(sharedLayer);  // Apply LeakyReLU activation

    sharedLayer = tf.layers.batchNormalization().apply(sharedLayer);
    sharedLayer = tf.layers.dropout({ rate: 0.4 }).apply(sharedLayer);

    // Regular numbers output branch (7 numbers)
    const regularNumbersOutput = tf.layers.dense({ units: 7, activation: 'sigmoid', name: 'regularNumbers' }).apply(sharedLayer);

    // Powerball output branch (1 number)
    const powerballOutput = tf.layers.dense({ units: 1, activation: 'sigmoid', name: 'powerball' }).apply(sharedLayer);

    // Create the model with two outputs
    const model = tf.model({
        inputs: inputLayer,
        outputs: [regularNumbersOutput, powerballOutput]
    });

    // Compile the model with separate loss functions for each output
    model.compile({
        optimizer: 'rmsprop',
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
        callbacks: tf.callbacks.earlyStopping({ patience: 10 })  // Increased patience for early stopping
    });

    console.log('Training complete.');
    return history;
}

// Predict the next draw numbers
async function predictNextDraw(model, lastDraws, timeSteps = 10) {
    // Ensure that you are passing exactly 'timeSteps' past draws of 7 regular numbers and 1 powerball (8 values in total)
    const input = lastDraws.map(draw => {
        const regularNumbers = draw.slice(0, 7).map(num => normalizeNumber(num, MAX_NUMBER));  // Slice to take only 7 numbers
        const powerball = normalizeNumber(draw[7], MAX_POWERBALL);  // Take only 1 powerball
        return regularNumbers.concat([powerball]);  // Concatenate regular numbers and powerball
    });

    const inputTensor = tf.tensor3d([input]);  // Ensure input has shape [1, timeSteps, 8]

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
    const filePath = './powerball_results_subset.csv';  // Path to your CSV file
    const dataset = await loadCSVData(filePath);  // Load data from CSV

    if (dataset.length === 0) {
        console.error('No data found or invalid file format.');
        return;
    }

    const timeSteps = 10;  // Number of past draws to use for sequence-based prediction

    // Prepare the data for training
    const { xs, regularNumberYs, powerballYs } = prepareData(dataset, timeSteps);

    // Create the model
    const model = createModel(timeSteps);

    // Train the model
    await trainModel(model, xs, regularNumberYs, powerballYs);

    // Predict the next lotto numbers using the last 'timeSteps' draws from your dataset
    const lastDraws = dataset.slice(dataset.length - timeSteps).map(draw => draw[0].concat([draw[1]]));  // Get last 10 draws
    await predictNextDraw(model, lastDraws, timeSteps);
}

// Run the main function
main();