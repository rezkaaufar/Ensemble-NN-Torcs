import cicontest.algorithm.abstracts.AbstractDriver;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.extras.ABS;
import cicontest.torcs.controller.extras.AutomatedClutch;
import cicontest.torcs.controller.extras.AutomatedGearbox;
import cicontest.torcs.controller.extras.AutomatedRecovering;
import cicontest.torcs.genome.IGenome;
import scr.Action;
import scr.SensorModel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.UUID;

import java.nio.file.*;

public class DefaultDriver extends AbstractDriver {

    private TRACK_NAME trackName;
    // test
    private NeuralNetwork nnAI;
    private NeuralNetwork nnHuman;

    // arrows pressed and released
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean rightPressed = false;
    private boolean leftPressed = false;
    private boolean upReleased = false;
    private boolean downReleased = false;
    private boolean rightReleased = false;
    private boolean leftReleased = false;

    private boolean currentSteeringRight = false;
    private boolean currentSteeringLeft = false;

    private double rightCumulSteering=0.0D;
    private double leftCumulSteering=0.0D;
    FocusFrame focusFrame;

    private PrintWriter pw;
    private static PrintWriter pw2;
    private static PrintWriter pw3;
    private BufferedReader reader;

    List<String> lines = null;
    private int count;

    // change this value to choose whether to simulate or control the car
    private boolean simulate = false;

    // change this value to rule based
    private boolean furthestSensor = false;
    private boolean recovery =false;
    //parameters for the sensor model :
    double minFurthestSpeed = 52;
    double brakeDistanceFactor=0.45;
    double brakeFurthestFactor=71;
    double accelerateFurthestFactor=0.3;

    double evadeDistance =7.60;//7.6
    double evadeSpaceOvertake=4;//13.8
    boolean evadeOutside=true;
    double evadeBrake =0.416;
    double evadeAccelerate=0.885;
    double evadeSteer= 0.228;


    // change this value to simulate test on trained neural net
    private boolean testNeural = false;
    private boolean trainNeural = false;
    private boolean saveNeural = false;

    private double[] previous_outputs={0.0D,0.0D,0.0D};

    private boolean complexNeural = true;
    private boolean crossvalidateWeight =false;
    private static int numLoop = 0;
    private boolean first_cross = true;
    private boolean waitRestart =false;
    private double step = 0.1;
    private static int maxLoop = 0;
    //The weights are overide when crossvalidateWeight is true

    private double ruleBasedWeight = 0.7;
    private double nnAIWeight = 0.3;
    private double nnHumanWeight = 0.0;


    // for GP
    private boolean useGP=false;//only if complexNeural =true and crossvalidateWeight=false
    private static GP2 genP;
    boolean useGPevade=false;
    //EVADE OPPONENT 1
    //private double[] GPMinBounds= {0.0,0.0};//distance , direction
    //private double[] GPMaxBounds= {30.0,18.0};//distance , direction
    //EVADE OPPONENT 2
    private double[] GPMinBounds= {0.0,0.0,0.0,0.0,0.0,0.0};//[distance, spaceOvertake ,outsideOvertake,evadeBrake,evadeAccelerate, evadeSteer]
    private double[] GPMaxBounds= {60.0,15.0,1.0,1.0,1.0,1.0};//[distance, spaceOvertake ,outsideOvertake,evadeBrake,evadeAccelerate,evadeSteer]
    boolean useGPFurthest=false;
    //FURTHEST SENSORS
    //private double[] GPMinBounds= {0.0,0.0,0.0,0.0};//[minSpeed,brakeDistanceFactor, brakeFactor ,accelerateFactor]
    //private double[] GPMaxBounds= {100.0,2.0,100.0,1.0};//[minSpeed,brakeDistanceFactor, brakeFactor ,accelerateFactor]

    private boolean newGeneration =true;
    private static double[] lapTimes;

    //SWARM INTELLIGENCE
    private boolean useSwarm = true; //should always be set to true
   // private String swarmFolder ="Swarm/";
    //TODO: set the folder to the correct value
    private String swarmFolder ="/Users/ci_course/torcs/shared_folder/hV2WAKPWYkuH/";
    private int actionNumber=0;
    private PrintWriter pw4;
    private BufferedReader reader4;
    String uniqueID;
    private boolean isBlocking=false;
    //TODO: clean the file when track ends

    // change this value to determine how many hidden layers and the size of it
    private int[] layersConfig = {50, 50, 25, 25};

    //Use to know if this is the first time we enter the controller:
    boolean raceStarted = false;
    double startTime;

    public DefaultDriver() {
        initialize();
    }

    private void cleanDirectory()
    {
        //clean the folder
        Path path = Paths.get(swarmFolder+"Swarm_communication.csv");
        try {
            Files.delete(path);
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", path);
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", path);
        } catch (IOException x) {
            // File permission problems are caught here.
            System.err.println(x);
        }
    }
    private void initialize() {
        this.enableExtras(new AutomatedClutch());
        this.enableExtras(new AutomatedGearbox());
        this.enableExtras(new AutomatedRecovering());
        this.enableExtras(new ABS());

        uniqueID = UUID.randomUUID().toString();

        if(useGP) {
            if(numLoop==0){
                genP = new GP2(GPMinBounds,GPMaxBounds);
                lapTimes= new double[genP.populationSize];}
        }
        if(trainNeural)
        {
            nnAI = new NeuralNetwork(25, layersConfig, 3);
            nnHuman = new NeuralNetwork(25, layersConfig, 3);
            String[] trainingHuman = {trackName.A_SPEEDWAY,trackName.STREET1, trackName.CORKSCREW, trackName.E_TRACK6, trackName.E_TRACK2};
            String[] trainingAI = {trackName.F_SPEEDWAY, trackName.AALBORG, trackName.ALPINE1};
            //nnAI.Train(trainingAI);
            nnHuman.Train(trainingHuman);
            if(saveNeural) {
                //nnAI.storeGenome("nnAI");
                nnHuman.storeGenome("nnHuman2");
            }
        }
        else
        {
            nnAI = new NeuralNetwork(true, "nnAI");
            nnHuman = new NeuralNetwork(true, "nnHuman2");
        }
        if(!simulate && !testNeural && !furthestSensor && !complexNeural) {
            focusFrame = new FocusFrame();
            focusFrame.requestFocus();
        }
        try {
            if(!simulate && !testNeural && !furthestSensor && !complexNeural) {
                pw = new PrintWriter(new File("test"+".csv"));
                pw.println("ACCELERATION,BRAKE,STEERING,SPEED,TRACK_POSITION,ANGLE_TO_TRACK_AXIS,TRACK_EDGE_0,TRACK_EDGE_1,TRACK_EDGE_2," +
                        "TRACK_EDGE_3,TRACK_EDGE_4,TRACK_EDGE_5,TRACK_EDGE_6,TRACK_EDGE_7,TRACK_EDGE_8,TRACK_EDGE_9,TRACK_EDGE_10," +
                        "TRACK_EDGE_11,TRACK_EDGE_12,TRACK_EDGE_13,TRACK_EDGE_14,TRACK_EDGE_15,TRACK_EDGE_16,TRACK_EDGE_17,TRACK_EDGE_18");
            } else if(simulate && !testNeural && !furthestSensor && !complexNeural) {
                reader = new BufferedReader(new FileReader(trackName.A_SPEEDWAY+".csv"));
                lines = new ArrayList<String>();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            if (crossvalidateWeight && !useGP)
            {
                if(pw2==null) {
                    pw2 = new PrintWriter(new File("weights" + ".csv"));
                    pw2.println("RULE_BASED,AI,HUMAN,TIME(s)");

                }
            }
            else if(useGP && ! crossvalidateWeight)
            {
                if(pw3==null) {
                    pw3 = new PrintWriter(new File("GP_results" + ".csv"));
                    if(useGPevade)
                        pw3.println("DISTANCE,SPACE OVERTAKE, OUTSIDE OVERTAKE, BRAKE, ACCELERATE,STEER ,TIME");
                        //pw3.println("DISTANCE,DIRECTION,TIME");
                    else if (useGPFurthest)
                        pw3.println("MIN SPEED, BRAKE DISTANCE FACTOR, BRAKE FACTOR ,ACCELERATE FACTOR,TIME");
                }
            }
            else if(useSwarm)
            {
                pw4 = new PrintWriter(new File(swarmFolder+"Swarm_communication" + ".csv"));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadGenome(IGenome genome) {
        if (genome instanceof DefaultDriverGenome) {
            DefaultDriverGenome myGenome = (DefaultDriverGenome) genome;
        } else {
            System.err.println("Invalid Genome assigned");
        }
    }

    @Override
    public double getAcceleration(SensorModel sensors) {
        double[] sensorArray = new double[4];
        double output = nnAI.getOutput(sensors);
        return 1;
    }

    @Override
    public double getSteering(SensorModel sensors) {
        Double output = nnAI.getOutput(sensors);
        return 0.5;
    }

    @Override
    public String getDriverName() {
        return "The Stig";
    }

    @Override
    public Action controlWarmUp(SensorModel sensors) {
        Action action = new Action();
//        if(GPtest){
//            return GPControl(action, sensors);
//        }

        if(complexNeural) {
            return complexControl(action, sensors);
        }
        else if(testNeural) {
            return neuralControl(action, sensors);
        }
        else if(furthestSensor) {
            return furthestSensorControl(action, sensors,null);
        } else if(!simulate) {
            return keyboardControl(action, sensors);
        }
        return defaultControl(action, sensors);
    }

    @Override
    public Action controlQualification(SensorModel sensors) {
        Action action = new Action();
        if(complexNeural) {
            return complexControl(action, sensors);
        }
        else if(testNeural) {
            return neuralControl(action, sensors);
        } else if(furthestSensor) {
            return furthestSensorControl(action, sensors,null);
        }else if(!simulate) {
            return keyboardControl(action, sensors);
        }
        return defaultControl(action, sensors);
    }

    @Override
    public Action controlRace(SensorModel sensors) {
        Action action = new Action();
        if(complexNeural) {
            return complexControl(action, sensors);
        }
        else if(testNeural) {
            return neuralControl(action, sensors);
        } else if(furthestSensor) {
            return furthestSensorControl(action, sensors,null);
        } else if(!simulate) {
            return keyboardControl(action, sensors);
        }
        return defaultControl(action, sensors);
    }

    // used to test trained
    public Action neuralControl(Action action, SensorModel sensors) {
        if (action == null) {
            action = new Action();
        }
        Action neural = specificNeuralControl(action, sensors, nnHuman);
        return neural;
    }

    public Action specificNeuralControl(Action action, SensorModel sensors, NeuralNetwork net) {
        if (action == null) {
            action = new Action();
        }

        double[] predicted_outputs = net.predict(sensors,previous_outputs);

        if (sensors.isFinished()) {
            action.restartRace = true;
        }

        action.accelerate = predicted_outputs[0];
        action.brake = predicted_outputs[1];
        action.steering = predicted_outputs[2];
        //System.out.println("predicted= Acc " + predicted_outputs[0] + " Brake " + predicted_outputs[1] + " Steering " + predicted_outputs[2]);


        return action;
    }
    // used to simulate the data
    @Override
    public Action defaultControl(Action action, SensorModel sensors) {
        if(!raceStarted)
        {
            raceStarted=true;
            startTime=System.currentTimeMillis();
        }
        double time_elapsed=(System.currentTimeMillis()- startTime);
        if(sensors.getLaps()==1 || time_elapsed>300000)//5 min = 5 * 60 * 1000 ms = 300 000
        {
            //System.out.println("Track is finished in "+ NeuralNetwork.convertTime(time_elapsed,true));
            raceStarted=false;
            action.restartRace=true;
        }


        count += 1;
        if (action == null) {
            action = new Action();
        }

        //if(lines.size() <= count){
        action.steering = DriversUtils.alignToTrackAxis(sensors, 0.5);
        if (sensors.getSpeed() > 60.0D) {
            action.accelerate = 1.0D;
            action.brake = 0.0D;
        }

        if (sensors.getSpeed() > 160.0D) {
            action.accelerate = 0.0D;
            action.brake = -1.0D;
        }

        if (sensors.getSpeed() <= 60.0D) {
            action.accelerate = (80.0D - sensors.getSpeed()) / 80.0D;
            action.brake = 0.0D;
        }

        if (sensors.getSpeed() < 30.0D) {
            action.accelerate = 1.0D;
            action.brake = 0.0D;
        }
        /*} else {
            String act = lines.get(count);
            String[] acts = act.split(",");

            action.steering = Double.parseDouble(acts[2]);
            action.brake = Double.parseDouble(acts[1]);
            action.accelerate = Double.parseDouble(acts[0]);
        }
*/
        return action;
    }

    private Action evadeOpponents(Action currentAction,SensorModel sensors, double[] edges, double[] GPParam, int edgesIndex){
        double[] opponents = sensors.getOpponentSensors();
        double distance =GPParam[0];
        double newDirection =GPParam[1];
        if(opponents[9] < distance || opponents[8] < distance || opponents[10] < distance){
            double direction;
            if(edges[6] < edges[12])
                direction = edgesIndex + newDirection;
            else{
                direction = edgesIndex - newDirection;
            }
            currentAction.steering = ((90.0 - (direction * 10.0)/180) * 3.14);
        }
        return currentAction;
    }
    private Action evadeOpponents2(Action currentAction,SensorModel sensors, double[] edges,
                                   double distance,double spaceOvertake,boolean outsideOvertake,double brake,double accelerate, double steer, int edgesIndex)
    {
        //GPParam = [distance, spaceOvertake ,outsideOvertake,keepDistance]

        double[] opponents = sensors.getOpponentSensors();

        int directionToTrackAxis = (int)Math.round((sensors.getAngleToTrackAxis()*180.0)/(100.0*Math.PI));
        int leftSensorsIndex = Math.max(0,0+directionToTrackAxis);
        int rightSensorsIndex = Math.min(18,18-directionToTrackAxis);

        double leftSensors = edges[leftSensorsIndex];
        double rightSensors = edges[rightSensorsIndex];


        /*
        * sensor 0 : -180° (behind)
        * sensor 9: -90° (left)
        * sensor 18: 0° (front)
        * sensor 27: 90° (right)
        * */
        boolean leftOvertake= (opponents[16] < distance)||(opponents[17] < distance) || (sensors.getAngleToTrackAxis()>0 &&
                opponents[18] < distance );
        boolean rightOvertake = (opponents[19] < distance)  || (opponents[20] < distance)|| (sensors.getAngleToTrackAxis()<=0 &&
                opponents[18] < distance );

        if(leftOvertake || rightOvertake) {
            if(leftOvertake)
            {
                if(leftSensors>spaceOvertake ) {
                    currentAction.steering = evadeSteer; //left
                }
                else if (outsideOvertake && (rightSensors>spaceOvertake) ) {
                    currentAction.steering = -1 * evadeSteer; //right
                }
            }
            else {
                if (rightSensors > spaceOvertake) {
                    currentAction.steering = -1 * evadeSteer; //right
                }
                else if (outsideOvertake && (leftSensors > spaceOvertake)) {
                    currentAction.steering = evadeSteer; //left
                }
            }
            currentAction.accelerate = accelerate;
            currentAction.brake = brake;


        }
        return currentAction;
    }

    // used to simulate the data
    public Action furthestSensorControl(Action action, SensorModel sensors, double[] GPSpecies) {

        if (action == null) {
            action = new Action();
        }

        double furthest = 0;
        int edgesIndex = 0;
        double[] edges = sensors.getTrackEdgeSensors();

        if(recovery && (Math.abs(sensors.getAngleToTrackAxis())<Math.PI/8.0))
            recovery=false;

        boolean isReversed =Math.abs(sensors.getAngleToTrackAxis()) > Math.PI/2.0 || recovery;
        if(isReversed)
            recovery=true;

        for(int i =0;i<edges.length; i++){
            if(edges[i] > furthest && !isReversed ){
                furthest = edges[i];
                edgesIndex = i;
            }
        }
        if( recovery && sensors.getAngleToTrackAxis() < -Math.PI/8.0  )
            edgesIndex=18;

        action.steering = ((90.0 - (double)edgesIndex * 10.0)/180) * 3.14;
//            action.steering = DriversUtils.alignToTrackAxis(sensors, 0.5);


        if (sensors.getSpeed() < 400.0D) {
            action.accelerate = 1.0D;
            action.brake = 0.0D;
        }

        if(useGPFurthest)
        {
            minFurthestSpeed=GPSpecies[0];
            brakeDistanceFactor=GPSpecies[1];
            brakeFurthestFactor=GPSpecies[2];
            accelerateFurthestFactor=GPSpecies[3];

        }
        //DEFAULT CONTROLLER
        if(edges[9] < brakeDistanceFactor*sensors.getSpeed() && sensors.getSpeed() > minFurthestSpeed && !isReversed){//>40
            action.brake = (brakeFurthestFactor)/((double)edges[9]); // 15
            action.accelerate = accelerateFurthestFactor;
        }

        //EVADE OPPONENTS

        if(GPSpecies!= null && useGP) {
            if(useGPevade) {
                evadeDistance = GPSpecies[0];
                evadeSpaceOvertake = GPSpecies[1];
                evadeOutside = (GPSpecies[2]<0.5);
                evadeBrake = GPSpecies[3];
                evadeAccelerate = GPSpecies[4];
                evadeSteer=GPSpecies[5];
            }}
        if(!isReversed) {
            Action evadeAction = evadeOpponents2(copy_Action(action), sensors, edges, evadeDistance, evadeSpaceOvertake
                    , evadeOutside, evadeBrake,evadeAccelerate,evadeSteer, edgesIndex);

            action.steering = evadeAction.steering;
            action.accelerate = evadeAction.accelerate;
            action.brake = evadeAction.brake;
        }

        //USE SWARM
        if(useSwarm && !isReversed) {
            Action swarmAction = SwarmControl(action,sensors, edgesIndex);
            //Action swarmAction = BlockOpponent(copy_Action(action),sensors,edgesIndex,evadeDistance,evadeSpaceOvertake);
            action.steering= swarmAction.steering;
        }
        return action;
    }

    public double[] use_genetic_algorithm(SensorModel sensors, Action action)
    {
        int popIndex = numLoop%(genP.populationSize);
        //if we wait for the restart, we do nothing
        if(!waitRestart) {

            //New population
            if (numLoop != 0 && popIndex == 0 && newGeneration) {
                newGeneration=false;
                if ((numLoop + genP.populationSize) < (DefaultDriverAlgorithm.numberRun))
                    genP.nextGeneration(lapTimes);
                else {
                    for(int i=0; i<genP.populationSize; ++i)
                    {
                        String s="";
                        double[] res= genP.population.get(i);
                        s+=res[0];
                        for(int j=1; j< res.length; ++j)
                            s+=","+res[j];
                        s+=","+lapTimes[i];
                        pw3.println(s);
                    }
                    pw3.close();
                    System.exit(0);
                }
            }
            else if ( !newGeneration && (popIndex != 0) )
                newGeneration=true;

            boolean offtrack=false;
            for (int i = 0; i< (sensors.getTrackEdgeSensors().length); ++i)
                if(sensors.getTrackEdgeSensors()[i]==-1)
                    offtrack=true;

            //TODO: changed here; offtrack = true and time > 240, dmaage > 9500
            offtrack=false;
            //restart because track is succeed//more than 4 min // too much damage
            if (sensors.getLaps() == 1 || (sensors.getTime() > 90) || offtrack ||(sensors.getDamage() > 5000) || (sensors.isFinished())) {
                if (sensors.getLaps() == 1) {
                    lapTimes[popIndex] = sensors.getLastLapTime();
                }
                else
                    lapTimes[popIndex] = 300000; // 5 min : bad time/did not complete
                numLoop++;
                System.out.println("GP : " + (numLoop*100)/DefaultDriverAlgorithm.numberRun +" %");

                action.restartRace = true;
                waitRestart = true;

            }
        }

        return genP.population.get(popIndex);
    }
    public Action cross_validate(SensorModel sensors, Action action)
    {
        String s="";
        if(first_cross) {
            first_cross=false;
            boolean restart=false;
            //update the weights
            do {
                if(restart)
                    numLoop+=1;
                int max_ind = (int) (1.0 / step ) +1;
                if (maxLoop == 0)
                    maxLoop = max_ind * max_ind;

                ruleBasedWeight = step * (numLoop / max_ind);
                nnAIWeight = step * (numLoop % (max_ind));
                nnHumanWeight = 1 - nnAIWeight - ruleBasedWeight;

                restart=( nnHumanWeight>1.05 || nnHumanWeight<-0.05 ||nnAIWeight>1.05  || nnAIWeight<-0.05 ||ruleBasedWeight>1.05||
                        ruleBasedWeight<-0.05);
            }
            while(restart );
            restart=false;
            s= ruleBasedWeight + ","+ nnAIWeight +","+nnHumanWeight;
            System.out.println("TESTING FOR ruleBasedWeight,nnAIWeight,nnHumanWeight: "+s + " "+(int)((double)(numLoop*100)/(maxLoop))+ " %");
        }
        s= ruleBasedWeight + ","+ nnAIWeight +","+nnHumanWeight;

        boolean offtrack=false;
        for (int i = 0; i< (sensors.getTrackEdgeSensors().length); ++i)
            if(sensors.getTrackEdgeSensors()[i]==-1)
                offtrack=true;
        if(sensors.getLaps()==1 || offtrack ||((sensors.getTime())>240) || sensors.getDamage()>10000)
        {
            if(!waitRestart) {
                String stime = "";
                if(sensors.getLaps()==1)
                    stime=","+sensors.getTime();
                pw2.println(s+stime);
                if(ruleBasedWeight==1) {
                    pw2.close();
                    System.exit(0);
                }

                numLoop++;
                action.restartRace = true;
                waitRestart=true;
            }
        }
        return action;
    }

    public Action BlockOpponent(Action action, SensorModel sensors,int edgesIndex,double distance,  double enoughSpace)
    {
        double[] opponentSensors = sensors.getOpponentSensors();
        double[] edges = sensors.getTrackEdgeSensors();
        double steerRight =((90.0 - (edgesIndex +1) * 10.0)/180) * 3.14;
        double steerLeft=((90.0 - (edgesIndex -1) * 10.0)/180) * 3.14;

        int directionToTrackAxis = (int)Math.round((sensors.getAngleToTrackAxis()*180.0)/(100.0*Math.PI));
        int leftSensorsIndex = Math.max(0,0+directionToTrackAxis);
        int rightSensorsIndex = Math.min(18,18-directionToTrackAxis);

        double leftSensors = edges[leftSensorsIndex];
        double rightSensors = edges[rightSensorsIndex];

        double tunedEnoughSpace = 0.9*enoughSpace;
        double tunedDistance = distance *1.5 ;

        boolean opponentLeft = (opponentSensors[1]<tunedDistance) ||(opponentSensors[2]<tunedDistance);
        boolean opponentRight = (opponentSensors[34]<tunedDistance) ||(opponentSensors[35]<tunedDistance);

        if(opponentSensors[0]<tunedDistance)
            return action;
        if(opponentLeft)
        {
            if(leftSensors>tunedEnoughSpace) {
                action.steering = steerLeft;
                return action;
            }
        }
        if(opponentRight)
        {
            if(rightSensors>tunedEnoughSpace){
                action.steering=steerRight;
                return action;
            }
        }
        return action;
    }
    public Action SwarmControl(Action action, SensorModel sensors,int edgesIndex)
    {
        int actionState = (actionNumber%3);
        if(actionState==0)
        {
            try {
                pw4= new PrintWriter(new FileWriter(swarmFolder+"Swarm_communication"+".csv",true));//add to old file

                pw4.println(uniqueID+","+sensors.getRacePosition());
                pw4.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        else if (actionState==1 )
        {
            List<String> lines4= new ArrayList<>();
            int allyPosition = -1;
            int myPreviousPos = -1;
            try {
                reader4 = new BufferedReader(new FileReader(swarmFolder+"Swarm_communication.csv"));
                lines4 = new ArrayList<String>();
                String line4 = null;

                while ((line4 = reader4.readLine()) != null) {
                    lines4.add(line4);
                }
                reader4.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(lines4.size()>1)
            {
                String[] l1 = lines4.get(0).split(",");
                String[] l2 = lines4.get(1).split(",");

                if(! l1[0].equals(uniqueID)) {
                    allyPosition = Integer.parseInt(l1[1]);
                    myPreviousPos= Integer.parseInt(l2[1]);
                }
                else if(! l2[1].equals(uniqueID)) {
                    allyPosition = Integer.parseInt(l2[1]);
                    myPreviousPos = Integer.parseInt(l1[1]);
                }
                else
                    throw new Error("Ally has not written in file");

            }
            if(allyPosition!=-1 && (myPreviousPos !=-1))
            {
                if( (myPreviousPos>allyPosition) || (myPreviousPos==allyPosition && isBlocking) )
                    isBlocking=true;

                else
                    isBlocking=false;


            }

        }
        else if(actionState==2)
        {
            //reset the file
            try {
                //Only one of the two driver delete the file
                pw4 = new PrintWriter(new FileWriter(swarmFolder + "Swarm_communication.csv"),false);//add to old file
                pw4.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(isBlocking)
            return BlockOpponent(copy_Action(action), sensors, edgesIndex, evadeDistance, evadeSpaceOvertake);
        else
            return action;
    }
    public Action complexControl(Action action, SensorModel sensors) {
        /*if(sensors.isFinished() && useSwarm) {
            cleanDirectory();
            System.out.println("CLEAN ------------------------------");
        }*/

        double time = System.currentTimeMillis();
        double[] gpParam=null;
        boolean carReversed =Math.abs(sensors.getAngleToTrackAxis())>= Math.PI/2.0;
        if(crossvalidateWeight && !useGP)
        {
            //set the weights and decide if the race needs to be restarted
            Action cross_action = cross_validate(sensors,copy_Action(action));
            action.restartRace=cross_action.restartRace;
        }
        if(useGP && ! crossvalidateWeight)
        {
            Action gpAction = copy_Action(action);
            //modifies the gpAction
            gpParam = use_genetic_algorithm(sensors,gpAction);
            action.restartRace=gpAction.restartRace;
        }
        Action ruleBaseAction= furthestSensorControl(copy_Action(action), sensors,gpParam);
        Action aiNeuralControl = specificNeuralControl(copy_Action(action), sensors, nnAI);
        Action humanNeuralControl = specificNeuralControl(copy_Action(action), sensors, nnHuman);

        //overide if problem
        if(carReversed) {
            action.steering = ruleBasedWeight * ruleBaseAction.steering + nnAIWeight * aiNeuralControl.steering
                    + nnHumanWeight * humanNeuralControl.steering;
            action.brake = ruleBasedWeight * ruleBaseAction.brake + nnAIWeight * aiNeuralControl.brake
                    + nnHumanWeight * humanNeuralControl.brake;
            action.accelerate = ruleBasedWeight * ruleBaseAction.accelerate + nnAIWeight * aiNeuralControl.accelerate
                    + nnHumanWeight * humanNeuralControl.accelerate;
        }
        else// rule base because the car face the wrong side
        {
            action.steering=ruleBaseAction.steering;
            action.brake=ruleBaseAction.brake;
            action.accelerate= ruleBaseAction.accelerate;
        }
        actionNumber+=1;
        return action;
    }

    // used to get human data
    public Action keyboardControl(Action action, SensorModel sensors) {

        System.out.println("Damage = "+ sensors.getDamage());
        double steerSensitivity=0.01D;
        if (action == null) {
            action = new Action();
        }

        if(upPressed) action.accelerate = 1.0D;
        if(upReleased) {
            action.accelerate = 0.0D;
            upReleased = false;
        }
        if(downPressed) {
            action.accelerate = 0.0D;
            action.brake = 1.0D;
        }
        if(downReleased) {
            action.brake = 0.0D;
            downReleased = false;
        }
        if(rightPressed) {
            if(rightCumulSteering>=0) {
                rightCumulSteering = -0.3D;
            }
            else if(rightCumulSteering>(steerSensitivity-1))
                rightCumulSteering-=steerSensitivity;
            else
                rightCumulSteering = -1.0D;
            action.steering = rightCumulSteering;
        }
        if(rightReleased) {
            action.steering = 0.0D;
            rightReleased = false;
            rightCumulSteering=0.0D;
        }
        if(leftPressed)
        {
            if(leftCumulSteering<=0)
                leftCumulSteering = 0.3D;
            else if(leftCumulSteering<(1-steerSensitivity))
                leftCumulSteering += steerSensitivity;
            else
                leftCumulSteering = 1.0D;
            action.steering = leftCumulSteering;

        }
        if(leftReleased) {
            action.steering = 0.0D;
            leftReleased = false;
            leftCumulSteering=0.0D;
        }

        String s = "";
        s += action.accelerate + "," + action.brake + "," + action.steering + "," + sensors.getSpeed() + "," +
                sensors.getTrackPosition() + "," + sensors.getAngleToTrackAxis();
        double[] track_edge_sensors= sensors.getTrackEdgeSensors();
        for(int i=0; i<track_edge_sensors.length;++i) {
            s += "," + track_edge_sensors[i];
        }
        System.out.println("s: "+ s);
        pw.println(s);

        return action;
    }

    public class KeyboardListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {

            if(e.getKeyCode() == KeyEvent.VK_UP) {
                upPressed = true;
                upReleased = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                downPressed = true;
                downReleased = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = true;
                rightReleased = false;
                currentSteeringRight = true;
                currentSteeringLeft = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = true;
                leftReleased = false;
                currentSteeringRight = false;
                currentSteeringLeft = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

            if(e.getKeyCode() == KeyEvent.VK_UP) {
                upReleased = true;
                upPressed = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                downReleased = true;
                downPressed = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if(currentSteeringRight)
                    rightReleased = true;
                rightPressed = false;
                currentSteeringRight = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                if(currentSteeringLeft)
                    leftReleased = true;
                leftPressed = false;
                currentSteeringLeft = false;
            }
        }
    }

    public class FocusPanel extends JPanel {
        public FocusPanel() {
            super();
        }
    }

    public class FocusFrame extends JFrame {
        public FocusFrame() {
            this.setFocusable(true);
            this.setSize(400, 500);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setVisible(true);
            this.setTitle("Focus to Control");
            this.setContentPane(new FocusPanel());
            this.addKeyListener(new KeyboardListener());
        }
    }

    public Action copy_Action (Action ac)
    {
        Action acc = new Action();
        acc.accelerate=new Double(ac.accelerate);
        acc.brake=new Double(ac.brake);
        acc.clutch=new Double(ac.clutch);
        acc.steering=new Double(ac.steering);
        acc.restartRace=new Boolean(ac.restartRace);
        acc.gear=new Integer(ac.gear);
        acc.focus=new Integer(ac.focus);

        return acc;
    }
}
