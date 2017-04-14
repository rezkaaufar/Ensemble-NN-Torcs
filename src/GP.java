//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Random;
//
//import static cern.clhep.Units.s;
//
///**
// * Created by daniel on 18-11-2016.
// */
//public class GP {
//
//    public ArrayList<ArrayList<String>> evolve() {
//
//        BufferedReader reader = null;
//        ArrayList<String> lines = null;
//        // data preparation
//
//        try {
//            reader = new BufferedReader(new FileReader("A_Speedway_34_52.csv"));
//
//            lines = new ArrayList<String>();
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                lines.add(line);
//            }
//        } catch (FileNotFoundException e1) {
//            e1.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
////        System.out.print(lines);
//
//        ArrayList<ArrayList<String>> population = new ArrayList<ArrayList<String>>();
//        population.add(lines);
//        for(int i = 0; i<99; i++){
//            population.add(mutation(lines, 1));
//        }
//        return population;
//    }
//
//    public ArrayList<ArrayList<String>> nextGeneration(ArrayList<ArrayList<String>> population, double[] lapTimes){
//        // insert new mutations
//
//
//
//        for(int i = 0; i<79; i++){
//            population.add(mutation(lines, 20));
//        }
//
//        return population;
//    }
//
//
//    private ArrayList<String> mutation(ArrayList<String> raceData, int index) {
//        Random r = new Random();
//        double rangeMin = 0.9;
//        double rangeMax = 1.1;
//        double randomValue;
//
////        System.out.println(raceData.get(1));
//
//        StringBuilder strBuilder = new StringBuilder();
//        String act = "";
//        String[] acts = null;
//
//        for(int i = index; i<raceData.size();i++){
//            act = raceData.get(i);
//            acts = act.split(",");
//            for(int j=3; j<6; j++){
//                randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
//                acts[j] = String.valueOf(Double.parseDouble(acts[j]) * randomValue);
//            }
//            strBuilder = new StringBuilder();
//            for (int k = 0; k < acts.length-1; k++) {
//                strBuilder.append(acts[k] + ',');
//            }
//            strBuilder.append(acts[acts.length-1]);
//            raceData.set(i, strBuilder.toString());
//        }
//
////        System.out.print(raceData.get(1));
//        return raceData;
//    }
//}
