import org.encog.Encog;
import org.encog.engine.network.activation.ActivationElliott;
import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import scr.SensorModel;

import java.io.*;
import java.util.*;
/*
* TODO:
* Use previous output as input: modify also predict
* */
public class NeuralNetwork implements Serializable {

    private static final long serialVersionUID = -88L;

    private BufferedReader reader;
    private List<String> lines = null;
    List<String> result = null;
    String line = null;
    BasicNetwork network = null;
    int inputs;
    int[] layerConfig;
    int outputs;

    int numberLoop = 2000;

    double tolerance= 0.0001;

    NeuralNetwork(int _inputs, int[] _layerConfig, int _outputs) {
        inputs=_inputs;
        layerConfig=_layerConfig;
        outputs=_outputs;
    }

    NeuralNetwork(boolean loadFromMemory, String filename) {
        if(loadFromMemory)
        {
            NeuralNetwork net = loadGenome(filename);
            lines=net.lines;
            result=net.result;
            line=net.line;
            network=net.network;
            inputs= net.inputs;
            layerConfig=net.layerConfig;
            outputs=net.outputs;
        }
    }

    public static String convertTime(double time,boolean bmilli)
    {
        int factor=1;
        if(bmilli)
        {
            factor=1000;
        }
        int hour = (int)(time/(3600*factor));
        int min = (int)(( time/60- hour*60)/factor);
        int sec = (int)((time - hour*3600 - min *60)/factor);
        int milli= (int) (time - hour*3600*factor - min *60*factor -sec*factor) ;
        String shour = hour<10? "0"+hour:""+hour;
        String smin = min<10? "0"+min:""+min;
        String ssec = sec<10? "0"+sec:""+sec;
        String smilli= milli<10?("000"+milli) : (milli<100?"00"+milli: "0"+milli);
        return (shour+"h "+smin+"m "+ssec+"s"+ (bmilli?(smilli+"ms"):""));
    }

    public void Train(String[] trainingSetNames)
    {
        // prepare training data, removing the first line of every .csv files
        lines = new ArrayList<>();
        prepareData(trainingSetNames);

        //TODO: put 25 input
        double TORCS_INPUT[][] = new double[lines.size()][25];
        double TORCS_IDEAL[][] = new double[lines.size()][3];

        // prepare the input
        String[] previousOutput = {"0.0","0.0","0.0"};
        for(int i = 0; i < lines.size(); i++) {
            String data[] = lines.get(i).split(",");
            for(int j = 0; j < data.length; j ++) {
                if(j<3)
                {
                    TORCS_INPUT[i][j] =Double.parseDouble(previousOutput[j]);
                    previousOutput[j]=data[j];
                }
                else {
                    TORCS_INPUT[i][j] = Double.parseDouble(data[j]);
                }
            }
        }

        //prepare the output
        for(int i = 0; i < lines.size(); i++) {
            String data[] = lines.get(i).split(",");
            for(int j = 0; j < 3; j ++) {
                TORCS_IDEAL[i][j] = Double.parseDouble(data[j]);
            }
        }

        // intialize the training set
        NeuralDataSet trainingSet = new BasicNeuralDataSet(TORCS_INPUT, TORCS_IDEAL);

        // setup the network
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(new ActivationLinear(), true, inputs));
        for (int i = 0; i < layerConfig.length; i ++) {
            network.addLayer(new BasicLayer(new ActivationTANH(), true, layerConfig[i]));
        }
        network.addLayer(new BasicLayer(new ActivationTANH(), true, outputs));
        network.getStructure().finalizeStructure();
        network.reset();

        // train the network
        final Train train = new ResilientPropagation(network, trainingSet);
        int epoch = 1;
        long startTime =System.currentTimeMillis();
        do {

            train.iteration();
            if(epoch%10==9) {
                //TIME is accurate only after 40% or 30 sec
                long currentTime=System.currentTimeMillis();
                long elapsed =(currentTime-startTime)/1000;
                double timetowait= ((double)numberLoop*elapsed)/epoch-elapsed;
                double temp = ((double)numberLoop*elapsed)/epoch;
                String s_time = convertTime(timetowait,false);
                System.out.println("Error: " + train.getError() + "// Time remaining: " + s_time+ " " +((int)epoch*100)/numberLoop + "%");
            }
            epoch++;

        } while(train.getError() > tolerance && epoch < numberLoop);
        System.out.println("Final Error:" + train.getError());

        //printTrainingResult(trainingSet);
        Encog.getInstance().shutdown();
    }

    public double[] predict_mirror(SensorModel sensors,double[] previousOutputs)
    {
        //previousOutputs correspond to previous accelerate, brake, steering
        double TORCS_INPUT[][] = new double[1][25];
        double TORCS_IDEAL[][] = new double[1][3];

        // prepare the input
        TORCS_INPUT[0][0] = previousOutputs[0];
        TORCS_INPUT[0][1] = previousOutputs[1];
        TORCS_INPUT[0][2] = -1*previousOutputs[2];//steering
        TORCS_INPUT[0][3] = sensors.getSpeed();
        TORCS_INPUT[0][4] = -1*sensors.getTrackPosition();
        TORCS_INPUT[0][5] = -1*sensors.getAngleToTrackAxis();
        double[] track_edge_sensors= sensors.getTrackEdgeSensors();
        for(int j = 0; j < track_edge_sensors.length; j ++) {
            TORCS_INPUT[0][j+6] = track_edge_sensors[18-j];
        }

        //prepare the output: unused value
        TORCS_IDEAL[0][0] = 0;
        TORCS_IDEAL[0][1] = 0;
        TORCS_IDEAL[0][2] = 0;

        // Data storage class in order to used them in .compute() function
        NeuralDataSet neuralData = new BasicNeuralDataSet(TORCS_INPUT, TORCS_IDEAL);

        MLData MLoutputs = null;
        for (MLDataPair neuralDataSet : neuralData) {
            MLoutputs = network.compute(neuralDataSet.getInput());
            //System.out.println("predicted=" + output.getData(0) + " " + output.getData(1) + " " + output.getData(2));
        }
        double[] outputs = new double[3];
        for(int i = 0; i<3; ++i)
        {
            outputs[i]= MLoutputs.getData(i);
            previousOutputs[i]=outputs[i];//This SHOULD modify the array passed as parameter
        }
        return outputs;
    }
    public double[] predict(SensorModel sensors,double[] previousOutputs)
    {
        //previousOutputs correspond to previous accelerate, brake, steering
        double TORCS_INPUT[][] = new double[1][25];
        double TORCS_IDEAL[][] = new double[1][3];

        // prepare the input
        TORCS_INPUT[0][0] = previousOutputs[0];
        TORCS_INPUT[0][1] = previousOutputs[1];
        TORCS_INPUT[0][2] = previousOutputs[2];
        TORCS_INPUT[0][3] = sensors.getSpeed();
        TORCS_INPUT[0][4] = sensors.getTrackPosition();
        TORCS_INPUT[0][5] = sensors.getAngleToTrackAxis();
        double[] track_edge_sensors= sensors.getTrackEdgeSensors();
        for(int j = 0; j < track_edge_sensors.length; j ++) {
            TORCS_INPUT[0][j+6] = track_edge_sensors[j];
        }

        //prepare the output: unused value
        TORCS_IDEAL[0][0] = 0;
        TORCS_IDEAL[0][1] = 0;
        TORCS_IDEAL[0][2] = 0;

        // Data storage class in order to used them in .compute() function
        NeuralDataSet neuralData = new BasicNeuralDataSet(TORCS_INPUT, TORCS_IDEAL);

        MLData MLoutputs = null;
        for (MLDataPair neuralDataSet : neuralData) {
            MLoutputs = network.compute(neuralDataSet.getInput());
            //System.out.println("predicted=" + output.getData(0) + " " + output.getData(1) + " " + output.getData(2));
        }
        double[] outputs = new double[3];
        for(int i = 0; i<3; ++i)
        {
            outputs[i]= MLoutputs.getData(i);
            previousOutputs[i]=outputs[i];//This SHOULD modify the array passed as parameter
        }
        return outputs;
    }
    public double getOutput(SensorModel a) {
        return 0.5;
    }

    //Store the state of this neural network
    public void storeGenome(String filename) {
        ObjectOutputStream out = null;
        try {
            //create the memory folder manually
            out = new ObjectOutputStream(new FileOutputStream("memory/" + filename + ".mem"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.writeObject(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load a neural network from memory
    public NeuralNetwork loadGenome(String filename) {

        // Read from disk using FileInputStream
       InputStream f_in = null;
        /*
        *         ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("/memory/" + filename + ".mem").getFile());*/
        f_in = getClass().getResourceAsStream("memory/" + filename + ".mem");


        // Read object using ObjectInputStream
        ObjectInputStream obj_in = null;
        try {
            obj_in = new ObjectInputStream(f_in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read an object
        try {
            if (obj_in != null) {
                return (NeuralNetwork) obj_in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void prepareData(String[] filenames) {
        // data preparation
        ClassLoader classLoader = getClass().getClassLoader();
        for(int i=0; i<filenames.length; ++i) {
            try {
                File file = new File(classLoader.getResource(filenames[i]+".csv").getFile());
                BufferedReader reader = new BufferedReader(new FileReader(file));
                reader.readLine(); // this will read the first line
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printTrainingResult(NeuralDataSet trainingSet) {
        System.out.println("Neural Network Results:");
        result = new ArrayList<String>();
        for (MLDataPair neuralDataSet : trainingSet) {
            MLData output = network.compute(neuralDataSet.getInput());
            result.add(output.getData(0) + "," + output.getData(1) + "," + output.getData(2));

            System.out.println("predicted=" + output.getData(0) + " " + output.getData(1) + " " + output.getData(2)
                    + ",expected=" + neuralDataSet.getIdeal().getData(0) + " " + neuralDataSet.getIdeal().getData(1)
                    + " " + neuralDataSet.getIdeal().getData(2));
        }
    }

}
