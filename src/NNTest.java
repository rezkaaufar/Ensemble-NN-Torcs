//import org.encog.Encog;
//import org.encog.engine.network.activation.ActivationSigmoid;
//import org.encog.ml.data.MLData;
//import org.encog.ml.data.MLDataPair;
//import org.encog.neural.data.NeuralData;
//import org.encog.neural.data.NeuralDataPair;
//import org.encog.neural.data.NeuralDataSet;
//import org.encog.neural.data.basic.BasicNeuralDataSet;
//import org.encog.neural.networks.BasicNetwork;
//import org.encog.neural.networks.layers.BasicLayer;
//import org.encog.neural.networks.training.Train;
//import org.encog.neural.networks.training.propagation.back.Backpropagation;
//
//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by rezka on 11/15/16.
// */
//public class NNTest {
//
//    public static void main(String[] args) {
//        BufferedReader reader;
//        List<String> lines = null;
//        // data preparation
//        try {
//            reader = new BufferedReader(new FileReader("A_Speedway_34_52.csv"));
//            lines = new ArrayList<String>();
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                lines.add(line);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        double TORCS_INPUT[][] = new double[lines.size()-1][21];
//        double TORCS_IDEAL[][] = new double[lines.size()-1][3];
//
//        // prepare the input
//        for(int i = 1; i < lines.size(); i++) {
//            String data[] = lines.get(i).split(",");
//            for(int j = 3; j < data.length - 1; j ++) {
//                TORCS_INPUT[i-1][j-3] = Double.parseDouble(data[j]);
//            }
//        }
//
//        //prepare the output
//        for(int i = 1; i < lines.size(); i++) {
//            String data[] = lines.get(i).split(",");
//            for(int j = 0; j < 3; j ++) {
//                TORCS_IDEAL[i-1][j] = Double.parseDouble(data[j]);
//            }
//        }
//
//        // intialize the training set
//        NeuralDataSet trainingSet = new BasicNeuralDataSet(TORCS_INPUT, TORCS_IDEAL);
//
//        // setup the network
//        BasicNetwork network = new BasicNetwork();
//        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 21));
//        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 12));
//        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 3));
//        network.getStructure().finalizeStructure();
//        network.reset();
//
//        // train the network
//        final Train train = new Backpropagation(network, trainingSet);
//
//        int epoch = 1;
//
//        do {
//
//            train.iteration();
//            System.out.println("Epoch #" + epoch +
//                    " Error:" + train.getError());
//            epoch++;
//
//        } while(train.getError() > 0.01);
//
//        System.out.println("Neural Network Results:");
//        for (MLDataPair neuralDataSet : trainingSet) {
//            final MLData output = network.compute(neuralDataSet.getInput());
//            System.out.println("predicted=" + output.getData(0) + " " + output.getData(1) + " " + output.getData(2)
//                    + ",train=" + neuralDataSet.getIdeal().getData(0) + " " + neuralDataSet.getIdeal().getData(0)
//                    + " " + neuralDataSet.getIdeal().getData(0));
//        }
//        Encog.getInstance().shutdown();
//    }
//}
