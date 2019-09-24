package me.utkarshpriyam.Network;

import java.io.*;
import java.util.*;

/**
 * This is the Perceptron class.
 * It represents a perceptron and uses
 * pdp (parallel distributive processing).
 *
 * This class can be instantiated either
 * by an array containing all layer sizes,
 * or by 2 integers for the input and output layer sizes along
 * with an array to that gives the sizes of the hidden layers.
 *
 * The pdp network reads its weight values and its inputs from
 * files named inputs.txt and weights.txt.
 *
 * The pdp network can run on three different modes:
 * Running, Training, and Testing
 *
 * In Running Mode (this is for actually using the network):
 *   - The network will output the final raw information it calculates
 *   - The network will not take any predicted output values
 *      - As a result, the network will neither train its weights
 *        not return an error value
 * WORK IN PROGRESS:
 * In Training Mode (as the name suggests, to train the network):
 *   - The network will take in both the inputs and the expected outputs
 *   - The network will automatically calculate its error and
 *     use the method of gradient descent to adjust its weights
 *     in order to reduce that aforementioned error
 *
 * TO BE IMPLEMENTED:
 * In Testing Mode (this is a blend of the previous two modes):
 *   - The network will take both the inputs and the expected outputs.
 *     However, as this is not a training exercise, the network will not
 *     update its weights in order to minimize error. It will simply calculate
 *     and return the error
 *
 * @author Utkarsh Priyam
 * @version 9/4/19
 */
public class Perceptron {
   /**
    * This boolean constant dictates whether to instantiate the
    * neuron and edge arrays as a full block (which has wasted space)
    * or as a ragged array (no wasted space)
    */
   private static final boolean GENERATE_RAGGED_ARRAYS = false;

   /**
    * This double value is the learning factor for this pdp network.
    * It is the step size that dictates how "fast" or "slowly" the
    * network adjusts its weights (whether it takes big or small
    * steps in the "downhill direction" (method of gradient descent).
    */
   private double lambda = 0.1;

   /**
    * TODO: (9/24/19) Add JavaDoc here
    */
   private final double lambdaChange = 1.01;

   /**
    * TODO: (9/24/19) Add JavaDoc here
    */
   private final double lambdaMaxCap = 1.5, lambdaMinCap = 0;

   /**
    * The layerCounts array stores the number of neurons
    * in each layer of the network.
    *
    * The activations array stores the activation values
    * of those neurons (stored as layer.neuron)
    *
    * The weights array stores the weights of the edges
    * of the network in a 3D array
    * (stored as layer.prevNeuron.nextNeuron)
    */
   private int[] layerCounts;
   private double[][] activations;
   private double[][][] weights;

   /**
    * This is the first constructor for the Perceptron class.
    * It takes in a single array that contains the counts
    * of the number of neurons in each layer of the network.
    *
    * @param layerCounts The array which contains the number of
    *                    neurons in each layer of the network
    *
    * @throws IllegalArgumentException This method throws an IllegalArgumentException
    *                                  if the parameter passed does not have at least
    *                                  2 values (for the input and output layers).
    */
   public Perceptron(int[] layerCounts)
   {
      // Throw an IllegalArgumentException if not enough layers (2) are passed
      if (layerCounts.length < 2)
         throw new IllegalArgumentException("not enough layers in network");

      // Adjust the inputs to ensure that every layer has at least 1 neuron
      layerCounts = setMinimumAllowed(layerCounts,1);

      // Store the layers array
      this.layerCounts = layerCounts;
      // Shorten the layer counts array to exclude the input and output counts
      int[] innerLayerCounts = new int[layerCounts.length - 2];
      for (int innerLayerIndex = 0; innerLayerIndex < innerLayerCounts.length; innerLayerIndex++)
         innerLayerCounts[innerLayerIndex] = layerCounts[innerLayerIndex + 1];

      // Generate the neuron and edge arrays
      generateNeuronsAndEdgesArrays(layerCounts[0],innerLayerCounts,layerCounts[layerCounts.length-1]);
   }

   /**
    * This is the second constructor for the Perceptron class.
    * It takes in 2 integers which tell the number of neurons
    * in the input and output layers of the network.
    * It also takes in a single array that contains the counts
    * of the number of neurons in each hidden layer of the network.
    *
    * @param numInputs         The number of neurons in the input layer of the network
    * @param hiddenLayersCount The array which contains the number of
    *                          neurons in each hidden layer of the network
    * @param numOutputs        The number of neurons in the output layer of the network
    */
   public Perceptron(int numInputs, int[] hiddenLayersCount, int numOutputs)
   {
      // Adjust the inputs to ensure that every layer has at least 1 neuron
      if (numInputs < 1)
         numInputs = 1;
      if (numOutputs < 1)
         numOutputs = 1;
      hiddenLayersCount = setMinimumAllowed(hiddenLayersCount,1);

      // Compact all the layers data into one single array
      layerCounts = new int[hiddenLayersCount.length + 2];
      layerCounts[0] = numInputs;
      System.arraycopy(hiddenLayersCount, 0, layerCounts, 1, hiddenLayersCount.length);
      layerCounts[layerCounts.length-1] = numOutputs;

      // Generate the neuron and edge arrays
      generateNeuronsAndEdgesArrays(numInputs,hiddenLayersCount,numOutputs);
   }

   /**
    * Goes through the given array and ensures that all values of the array
    * are at least as large as the given input integer
    * @param hiddenLayersCount The array to read and modify
    * @param minimumValue      The minimum allowed value
    *
    * @return The adjusted array
    */
   private int[] setMinimumAllowed(int[] hiddenLayersCount, int minimumValue)
   {
      int[] newArray = new int[hiddenLayersCount.length];
      for (int i = 0; i < hiddenLayersCount.length; i++)
         newArray[i] = Math.max(hiddenLayersCount[i], minimumValue);
      return newArray;
   }

   /**
    * This method handles the generation of the
    * neuron and edges arrays for this pdp network.
    *
    * It takes in the same three parameters
    * as the second constructor (which takes 3 parameters).
    *
    * It takes in 2 integers which tell the number of neurons
    * in the input and output layers of the network.
    * It also takes in a single array that contains the counts
    * of the number of neurons in each hidden layer of the network.
    *
    * @param numInputs         The number of neurons in the input layer of the network
    * @param hiddenLayersCount The array which contains the number of
    *                          neurons in each hidden layer of the network
    * @param numOutputs        The number of neurons in the output layer of the network
    */
   private void generateNeuronsAndEdgesArrays(int numInputs, int[] hiddenLayersCount, int numOutputs)
   {
      if (GENERATE_RAGGED_ARRAYS)
         // Generate Ragged Arrays
         generateArraysRagged(numInputs,hiddenLayersCount,numOutputs);
      else
         // Generate the "normal" full block arrays
         generateArraysRegular(numInputs,hiddenLayersCount,numOutputs);
   }

   /**
    * This method is called by generateNeuronsAndEdgesArrays(...)
    * if the boolean constant GENERATE_RAGGED_ARRAYS is true.
    * In this case, the neuron and edges arrays for this
    * pdp network are generated as ragged arrays.
    *
    * This method takes the exact same parameters as the
    * generateNeuronsAndEdgesArrays(...) method.
    *
    * It takes in 2 integers which tell the number of neurons
    * in the input and output layers of the network.
    * It also takes in a single array that contains the counts
    * of the number of neurons in each hidden layer of the network.
    *
    * @param numInputs         The number of neurons in the input layer of the network
    * @param hiddenLayersCount The array which contains the number of
    *                          neurons in each hidden layer of the network
    * @param numOutputs        The number of neurons in the output layer of the network
    */
   private void generateArraysRagged(int numInputs, int[] hiddenLayersCount, int numOutputs)
   {
      // Generate Neurons Array - Total Layers = Input + Hidden + Output = 2 + numHidden
      activations = new double[2 + hiddenLayersCount.length][];

      // Set Input Neurons Array length
      activations[0] = new double[numInputs];
      // Set Hidden Neurons Array length
      for (int i = 1; i < activations.length - 1; i++)
         activations[i] = new double[hiddenLayersCount[i]];
      // Set Output Neurons Array length
      activations[activations.length - 1] = new double[numOutputs];

      // Generate Edges Array - Total Layers = numNeurons - 1
      weights = new double[activations.length - 1][][];

      // Generate Second and Third Array Dimensions: Length = numNeurons in prev layer, next layer
      for (int m = 0; m < weights.length; m++)
         weights[m] = new double[activations[m].length][activations[m+1].length];
   }

   /**
    * This method is called by generateNeuronsAndEdgesArrays(...)
    * if the boolean constant GENERATE_RAGGED_ARRAYS is false.
    * In this case, the neuron and edges arrays for this
    * pdp network are not generated as ragged arrays.
    * They are instead generated as full 2D/3D blocks.
    *
    * This method takes the exact same parameters as the
    * generateNeuronsAndEdgesArrays(...) method.
    *
    * It takes in 2 integers which tell the number of neurons
    * in the input and output layers of the network.
    * It also takes in a single array that contains the counts
    * of the number of neurons in each hidden layer of the network.
    *
    * @param numInputs         The number of neurons in the input layer of the network
    * @param hiddenLayersCount The array which contains the number of
    *                          neurons in each hidden layer of the network
    * @param numOutputs        The number of neurons in the output layer of the network
    */
   private void generateArraysRegular(int numInputs, int[] hiddenLayersCount, int numOutputs)
   {
      // Count number maximum number of neurons in network
      // The number will serve as the 2nd, 2nd and 3rd dimensions
      // for the neurons, edges arrays
      int maxNumNeurons = Math.max(numInputs,numOutputs);
      for (int numNeurons: hiddenLayersCount)
         if (maxNumNeurons < numNeurons)
            maxNumNeurons = numNeurons;

      // Generate both arrays
      activations = new double[2 + hiddenLayersCount.length][maxNumNeurons];
      weights = new double[activations.length - 1][maxNumNeurons][maxNumNeurons];
   }

   /**
    * This method can be called by the network handler class in order to
    * have the pdp network read the weight values stored in the given file.
    *
    * This method takes the single parameter value weightsFile,
    * which has all of the weights for the network stored
    * in a specific ordering and organization within the file.
    *
    * @param weightsFile       The file which holds all of the weights for the network
    *
    * @throws RuntimeException This method throws a runtime exception if anything
    *                          goes wrong during the file-reading process.
    *                          This method also prints out the stack trace
    *                          of the original error.
    */
   void readWeights(File weightsFile)
   {
      try
      {
         // BufferedReader w can read all the weights out of the weightsFile file
         BufferedReader w = new BufferedReader(new FileReader(weightsFile));

         // Iterate through all the different weights layers
         for (int m = 0; m < weights.length; m++) {
            // Make sure the next line is not null
            String textLine = w.readLine();
            if (textLine == null)
               textLine = "";

            // Make a StringTokenizer to read the line
            StringTokenizer weightsLine = new StringTokenizer(textLine);

            // Now iterate over all the edges in the layer with index m

            // Iterate over the neurons in layer m first
            for (int jk = 0; jk < layerCounts[m]; jk++)
               // Then iterate over the neurons in layer m+1
               for (int ij = 0; ij < layerCounts[m + 1]; ij++)
               {
                  // If the weights line has more stuff, read it
                  if (weightsLine.hasMoreTokens())
                     weights[m][jk][ij] = parseDouble(weightsLine.nextToken());
                  else // Else default to a random double value in the range [low,high)
                     weights[m][jk][ij] = random(0,2);

                  System.out.println("w[" + m + "][" + jk + "][" + ij + "] = " + weights[m][jk][ij]);
               }
         }
      }
      catch (IOException ioException)
      {
         ioException.printStackTrace();
         throw new RuntimeException("The weights file is not formatted properly");
      }
   }

   /**
    * Generate a number uniformly at random in the interval [low,high).
    *
    * TODO (9/24/19) JavaDoc
    *
    * @param low
    * @param high
    * @return
    */
   private double random(double low, double high)
   {
      return (high - low) * Math.random() + low;
   }

   /**
    * This method can be called by the network handler class in order to
    * have the pdp network read the input values stored in the given file.
    *
    * This method takes the single parameter value inputsFile,
    * which has all of the inputs for the network stored
    * in a specific ordering and organization within the file.
    *
    * @param inputsFile       The file which holds all of the inputs for the network
    *
    * @throws RuntimeException This method throws a runtime exception if anything
    *                          goes wrong during the file-reading process.
    *                          This method also prints out the stack trace
    *                          of the original error.
    */
   private double[][] readInputs(File inputsFile)
   {
      try
      {
         // BufferedReader can read the inputsFile file
         BufferedReader in = new BufferedReader(new FileReader(inputsFile));

         // The file is formatted so the first line tells how many test cases there are
         int numTestCases = Integer.parseInt(in.readLine());
         double[][] inputs = new double[numTestCases][layerCounts[0]];

         // Iterate over all the test cases
         for (int iterator = 0; iterator < numTestCases; iterator++)
         {
            // Make sure the next line is not null
            String textLine = in.readLine();
            if (textLine == null)
               textLine = "";

            // Make a StringTokenizer to read the line
            StringTokenizer inputsLine = new StringTokenizer(textLine);

            for (int inputIndex = 0; inputIndex < layerCounts[0]; inputIndex++)
               // If the inputs line ran out, use 0 (default double value)
               if (inputsLine.hasMoreTokens())
                  // Else read from the inputs line
                  inputs[iterator][inputIndex] = parseDouble(inputsLine.nextToken());
         }

         return inputs;
      }
      catch (IOException ioException)
      {
         ioException.printStackTrace();
         throw new RuntimeException("The inputs file is not formatted properly");
      }
   }

   /**
    * This method can be called by the network handler class in order to
    * have the pdp network read the output values stored in the given file.
    *
    * This method takes the single parameter value outputsFile,
    * which has all of the outputs for the network stored
    * in a specific ordering and organization within the file.
    *
    * @param outputsFile       The file which holds all of the outputs for the network
    *
    * @throws RuntimeException This method throws a runtime exception if anything
    *                          goes wrong during the file-reading process.
    *                          This method also prints out the stack trace
    *                          of the original error.
    */
   private double[][] readOutputs(File outputsFile)
   {
      try
      {
         // BufferedReader can read the inputsFile file
         BufferedReader in = new BufferedReader(new FileReader(outputsFile));

         // The file is formatted so the first line tells how many test cases there are
         int numTestCases = Integer.parseInt(in.readLine());
         int numOutputs = layerCounts[layerCounts.length-1];
         double[][] outputs = new double[numTestCases][numOutputs];

         // Iterate over all the test cases
         for (int caseIndex = 0; caseIndex < numTestCases; caseIndex++)
         {
            // Make sure the next line is not null
            String textLine = in.readLine();
            if (textLine == null)
               textLine = "";

            StringTokenizer outputsLine = new StringTokenizer(textLine);

            for (int outputIndex = 0; outputIndex < numOutputs; outputIndex++)
               // If the outputs line ran out, use 0 (default double value)
               if (outputsLine.hasMoreTokens())
                  // Else read from the outputs line
                  outputs[caseIndex][outputIndex] = parseDouble(outputsLine.nextToken());
         }

         return outputs;
      }
      catch (IOException ioException)
      {
         ioException.printStackTrace();
         throw new RuntimeException("The output file is not formatted properly");
      }
   }

   /**
    * This method parses a double from a single string token.
    * If the token is not a double, then it just returns 0 by default.
    *
    * TODO (9/24/19) JavaDoc
    *
    * This method takes one String parameter nextToken
    *
    * @param nextToken The token to parse
    *
    * @return The parsed double, or 0 if the token cannot be parsed
    */
   private double parseDouble(String nextToken)
   {
      try
      {
         return Double.parseDouble(nextToken);
      }
      catch (NumberFormatException numberFormatException)
      {
         return 0;
      }
   }

   /**
    * This method can be called by the network handler class in order to
    * have the pdp network read the inputs values stored in the given file.
    *
    * This method takes the single parameter value inputsFile,
    * which has all of the inputs for the network stored
    * in a specific ordering and organization within the file.
    *
    * @param inputsFile        The file which holds all of the inputs for the network
    *
    * @return A 2D array of doubles, where each row represents a new test case. The
    *         array rows are sorted in the order that the inputs are given. Each row
    *         of the array will have as many elements as output neurons in the network.
    */
   protected double[][] runNetwork(File inputsFile)
   {
      // Get the inputs from the file
      double[][] inputs = readInputs(inputsFile);

      // Run the network on the inputs
      return runNetworkOnInputs(inputs);
   }

   /**
    * This method runs the network on the given inputs, and
    * it returns the output of the network on those inputs.
    *
    * This method takes exactly 1 parameter, a 2D array of doubles inputs,
    * which represents the inputs on which to run the network.
    *
    * @param inputs The inputs on which to run the network
    *
    * @return A 2D array of doubles which represents the
    *         output of the network for the given set of inputs
    */
   private double[][] runNetworkOnInputs(double[][] inputs)
   {
      // A 2D array is created with enough rows to store all the test cases individually
      double[][] outputs = new double[inputs.length][];

      // Iterate over all the test cases
      for (int testCaseIndex = 0; testCaseIndex < inputs.length; testCaseIndex++)
      {
         // Put the input values into the network
         activations[0] = inputs[testCaseIndex];

         // Calculate the activation values for all activation layers
         for (int n = 1; n < activations.length; n++)
            calculateActivations(n);

         // Index of output layer
         int outputLayerIndex = activations.length - 1;

         // Store the values of the output neurons into the 2D outputs array
         double[] activatedNeurons = activations[outputLayerIndex];
         int countOutputNeurons = layerCounts[outputLayerIndex];

         // Store the values into the outputs array
         outputs[testCaseIndex] = new double[countOutputNeurons];
         for (int outputNeuronIndex = 0; outputNeuronIndex < countOutputNeurons; outputNeuronIndex++)
            outputs[testCaseIndex][outputNeuronIndex] = activatedNeurons[outputNeuronIndex];
      }
      // Return the 2D outputs array
      return outputs;
   }

   /**
    * This method runs the network on the given inputs, and
    * it returns the output of the network on those inputs.
    *
    * This method takes exactly 1 parameter, a 1D array of doubles inputs,
    * which represents the inputs on which to run the network.
    *
    * @param inputs The inputs on which to run the network
    *
    * @return A 1D array of doubles which represents the
    *         output of the network for the given set of inputs
    */
   private double[] runNetworkOnInputs(double[] inputs)
   {
      // A 1D array to hold the outputs
      double[] outputs;

      // Put the input values into the network
      activations[0] = inputs;

      // Calculate the activation values for all activation layers
      for (int n = 1; n < activations.length; n++)
         calculateActivations(n);

      // Return the outputs array - Last row of activations array
      double[] calculatedOutputs = activations[activations.length - 1];
      outputs = new double[calculatedOutputs.length];

      for (int index = 0; index < calculatedOutputs.length; index++)
         outputs[index] = calculatedOutputs[index];

      return outputs;
   }

   /**
    * This method trains the network
    *
    * ADD JAVADOC FOR THIS METHOD
    *
    * TODO (9/16/19) JavaDoc
    *
    * @param inputsFile
    * @param outputsFile
    */
   protected void trainNetwork(File inputsFile, File outputsFile)
   {
      double[][] inputs = readInputs(inputsFile);
      double[][] outputs = readOutputs(outputsFile);
      if (inputs.length != outputs.length)
         throw new IllegalStateException("input and output files don't hold the same number of cases");

      System.out.println("inputs: " + Arrays.deepToString(inputs));
      System.out.println("outputs: " + Arrays.deepToString(outputs));

      for (int i = 0; i < 100000; i++)
         runTrainingStep(inputs,outputs);

      System.out.println();
      System.out.println("weights: " + Arrays.deepToString(weights));
      System.out.println("lambda: " + lambda);

      double[][] calculatedOutputs = runNetworkOnInputs(inputs);
      System.out.println("error: " + errorCalculator(outputs,calculatedOutputs)[0]);
   }

   private void runTrainingStep(double[][] inputs, double[][] outputs)
   {
      if (layerCounts.length != 3)
         throw new RuntimeException("training currently only works for A-B-1 networks");

      double[][] calculatedOutputs = runNetworkOnInputs(inputs);
      int numTestCases = outputs.length;
      for (int testCaseIndex = 0; testCaseIndex < numTestCases; testCaseIndex++)
      {
         // Run network on test case to store activation values into array
         runNetworkOnInputs(inputs[testCaseIndex]);

         // The weights adjustment array
         double[][][] weightAdjustments = new double[weights.length][weights[0].length][weights[0][0].length];
         // The error difference for this test case
         double errorDiff = outputs[testCaseIndex][0] - calculatedOutputs[testCaseIndex][0];

         for (int j = 0; j < layerCounts[1]; j++) // Middle Layer
            for (int i = 0; i < layerCounts[2]; i++) // Output Layer
            {
               int prevLayerLength = layerCounts[1];
               double activationValueUnbounded = 0;

               // Get the activation value unbounded - Currently Dot Product
               for (int layerElementsIndex = 0; layerElementsIndex < prevLayerLength; layerElementsIndex++)
                  activationValueUnbounded += activations[1][layerElementsIndex] * weights[1][layerElementsIndex][i];

               // These 2 lines handle this derivative --> d F_i/d W_abc
               double adjustment = neuronThresholdFunctionDeriv(activationValueUnbounded) * activations[1][j];
               // Multiply by learning factor and error diff to get delta W
               adjustment *= lambda * errorDiff;
               // Store delta W in array
               weightAdjustments[1][j][i] = adjustment;
            }

         for (int k = 0; k < layerCounts[0]; k++) // Input Layer
            for (int j = 0; j < layerCounts[1]; j++) // Middle Layer
            {
               int prevLayerLength = layerCounts[0], nextLayerLength = layerCounts[1];
               double activationValueUnbounded = 0, outputValueUnbounded = 0;

               // Get the activation value unbounded - Currently Dot Product
               for (int layerElementsIndex = 0; layerElementsIndex < prevLayerLength; layerElementsIndex++)
                  activationValueUnbounded += activations[0][layerElementsIndex] * weights[0][layerElementsIndex][j];

               // Get the output value unbounded - Currently Dot Product
               for (int layerElementsIndex = 0; layerElementsIndex < nextLayerLength; layerElementsIndex++)
                  outputValueUnbounded += activations[1][layerElementsIndex] * weights[1][layerElementsIndex][0];

               // Handle this derivative --> d f(h_j)/d W_abc
               double adjustment = neuronThresholdFunctionDeriv(outputValueUnbounded) * weights[1][j][0];
               //System.out.println("weights: " + weights[1][j][0]);
               // Handle this derivative --> d F_i/d W_abc
               adjustment *= neuronThresholdFunctionDeriv(activationValueUnbounded) * activations[0][k];
               //System.out.println("activations: " + activations[0][k]);
               // Multiply by learning factor and error diff to get delta W
               adjustment *= lambda * errorDiff;
               // Store delta W in array
               weightAdjustments[0][k][j] = adjustment;
               //System.out.println("weightsAdjustment[0][" + k + "][" + j + "]: " + weightAdjustments[0][k][j]);
            }

         // Scaled, Positive Error for this case
         double caseError = errorDiff * errorDiff / 2.0;
         //errorCalculator(outputs,calculatedOutputs)[0];

         for (int m = 0; m < weights.length; m++)
            for (int jk = 0; jk < layerCounts[m]; jk++)
               for (int ij = 0; ij < layerCounts[m+1]; ij++)
                  weights[m][jk][ij] += weightAdjustments[m][jk][ij];

         double[][] newCalculatedOutputs = runNetworkOnInputs(inputs);
         double newErrorDiff = outputs[testCaseIndex][0] - newCalculatedOutputs[testCaseIndex][0];
         double newCaseError = newErrorDiff * newErrorDiff / 2.0;
         //errorCalculator(outputs,newCalculatedOutputs)[0];

         if (newCaseError < caseError)
         {
            // Cap lambda (learning factor) to lambdaMaxCap
            if (lambda < lambdaMaxCap)
               lambda *= lambdaChange;
            calculatedOutputs = newCalculatedOutputs;
         }
         else
         {
            lambda /= lambdaChange;
            for (int m = 0; m < weights.length; m++)
               for (int jk = 0; jk < layerCounts[m]; jk++)
                  for (int ij = 0; ij < layerCounts[m+1]; ij++)
                     weights[m][jk][ij] -= weightAdjustments[m][jk][ij];
         }
      }
   }

   /**
    * TODO (9/24/19) JavaDoc
    */
   private double[] errorCalculator(double[][] expected, double[][] calculated) {
      if (expected.length != calculated.length || expected.length == 0)
         throw new IllegalArgumentException("The expected and calculated arrays must both have the same " +
               "non-zero number of test cases");

      if (expected[0].length != calculated[0].length)
         throw new IllegalArgumentException("The expected and calculated arrays must both have " +
               "the same number of output neuron values");

      double[] errors = new double[expected[0].length];
      for (int outputIndex = 0; outputIndex < errors.length; outputIndex++)
      {
         for (int testCaseIndex = 0; testCaseIndex < expected.length; testCaseIndex++) {
            double expectedValue = expected[testCaseIndex][outputIndex];
            double calculatedValue = calculated[testCaseIndex][outputIndex];

            double testCaseError = expectedValue - calculatedValue; // T0 - F0
            testCaseError = testCaseError * testCaseError; // (T0 - F0)^2

            errors[outputIndex] += testCaseError * testCaseError / 4.0; // ((T0 - F0)^4) / 4
         }
         errors[outputIndex] = Math.sqrt(errors[outputIndex]); // SQRT(sum of squares of errors)
      }

      return errors;
   }

   /**
    * This method calculates the activation of
    * all the neurons in the given layer.
    *
    * This method takes the single parameter layer,
    * which tells which layer index to evaluate.
    *
    * @param layer The index of the layer to evaluate.
    *              The layers are indexed from left to
    *              right, with the input layer as 0 and
    *              the output layer as layerCounts.length-1
    */
   private void calculateActivations(int layer)
   {
      // Get index of the previous layer
      int prevLayer = layer - 1;

      // Iterate over all the neurons in the layer with the given layer
      for (int layerElementIndex = 0; layerElementIndex < layerCounts[layer]; layerElementIndex++)
      {
         // All the neurons from the previous layer, stored in an array
         double[] prevNeurons = new double[layerCounts[prevLayer]];
         for (int prevNeuronIndex = 0; prevNeuronIndex < prevNeurons.length; prevNeuronIndex++)
            prevNeurons[prevNeuronIndex] = activations[prevLayer][prevNeuronIndex];

         // All the weights from the previous layer, stored in an array
         double[] prevWeights = new double[prevNeurons.length];

         for (int prevLayerElementIndex = 0; prevLayerElementIndex < layerCounts[prevLayer]; prevLayerElementIndex++)
            prevWeights[prevLayerElementIndex] = weights[prevLayer][prevLayerElementIndex][layerElementIndex];

         // DEBUG
         //System.out.print("a[" + layer + "][" + layerElementIndex + "] = f(");

         // The activation value of neuron indexed layerElementIndex is calculated - Currently Dot Product
         activations[layer][layerElementIndex] = 0;
         for (int index = 0; index < prevNeurons.length; index++)
         {
            activations[layer][layerElementIndex] += prevNeurons[index] * prevWeights[index];

            // DEBUG
            /*
            System.out.print("a[" + prevLayer + "][" + index + "]w[" + prevLayer + "][" + index  + "][" + layerElementIndex + "]");
            if (index != prevNeurons.length - 1)
               System.out.print(" + ");
            */
         }

         // Apply the threshold function
         activations[layer][layerElementIndex] = neuronThresholdFunction(activations[layer][layerElementIndex]);
         // DEBUG
         //System.out.println(")");
      }
   }

   /**
    * This method is the neuron threshold function
    * for this pdp network. It is designed to limit
    * the values passed through the network in order
    * to prevent the escalation of those values.
    * However, for simpler networks, this threshold
    * function can be omitted by making this method
    * simply return its parameter (input) directly.
    *
    * This method takes one parameter (input),
    * and applies a threshold function to
    * that double value in order to bound it
    * between some values (usually 0 to 1 or -1 to 1).
    *
    * @param neuronInput The input to be bounded
    *
    * @return The bounded version of the input
    */
   private double neuronThresholdFunction(double neuronInput)
   {
      // f(x) = x
      //return neuronInput;

      // f(x) = Sigmoid Function
      return 1.0 / (1.0 + Math.exp(-neuronInput));
   }

   /**
    * This method is the derivative of the neuron
    * threshold function for this pdp network. It
    * is used in the error calculations for the
    * neurons of the network.
    *
    * This method takes one parameter (input),
    * and applies the derivative of the threshold
    * function to that double value so that the
    * value can be used for the partial derivative
    * calculations later in the back propagation process.
    *
    * @param neuronInput The input to be processed in the
    *                    derivative of the threshold function
    *
    * @return The threshold function's derivative
    *         evaluated at the input double
    */
   private double neuronThresholdFunctionDeriv(double neuronInput)
   {
      // f(x) = x
      //return 1;

      // f(x) = Sigmoid Function
      double sigmoidValue = neuronThresholdFunction(neuronInput);
      return sigmoidValue * (1.0 - sigmoidValue);
   }
}
