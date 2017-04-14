import java.util.*;

/**
 * Created by daniel on 18-11-2016.
 */
public class GP2{

    public ArrayList<double[]> population = new ArrayList<>();
    int populationSize = 20;//20
    int numCrossover = 6;//6
    double mutationStrength = 0.1;
    int survivors = 8;//8
    double[] minBound;
    double[] maxBound;

    public GP2(double[] _minBound, double[] _maxBound ) {
        assert ((numCrossover+survivors)<populationSize);
        minBound=_minBound;
        maxBound=_maxBound;

        Random r = new Random();
        for(int i = 0; i<populationSize; i++){
            double[] species = new double[minBound.length];
            for(int j=0; j<species.length; ++j)
                species[j]= minBound[j] + (maxBound[j] - minBound[j]) * r.nextDouble();

            population.add(species);
        }
    }


    class Species
    {
        int index;// correspond to species population[index]
        double lapTime;
        public Species(int i, double lapT)
        {index=i;lapTime=lapT;}
        public String toString()
        {
            String s ="";
            for (int i=0; i< population.get(index).length; ++i)
                s+=i+": "+ population.get(index)[i];
            return ("Species " + index +" , laptime = "+ lapTime+ " "+ s);
        }
    }
    public Comparator<Species> SpeciesComp = new Comparator<Species>() {
        public int compare(Species o1, Species o2) {
            int res = 0;
            if(o1.lapTime - o2.lapTime<0)
                return -1;
            else if (o1.lapTime - o2.lapTime>0)
                return 1 ;
            else
                return 0;
        }
    };
    public void nextGeneration(double[] lapTimes){
        int mutations = populationSize - numCrossover - survivors;

        // keep fittest species
        ArrayList<Species> listSpecies = new ArrayList<>();

        for(int i = 0; i<lapTimes.length; ++i)
            listSpecies.add(new Species(i,lapTimes[i]));

        listSpecies.sort(SpeciesComp);
        for(int i = 0; i<listSpecies.size(); ++i)
           System.out.println(listSpecies.get(i).toString());

        //kill the population except the survivors
        ArrayList<double[]> populationSurvivors = new ArrayList<>();
        for (int j=0; j<survivors; ++j)
            populationSurvivors.add(population.get(listSpecies.get(j).index));

        //kill
        population= new ArrayList<>();
        for (int j=0; j<survivors; ++j)
            population.add(populationSurvivors.get(j));

        // insert new mutations
        Random r = new Random();
        for(int i = 0; i<mutations; i++){
            int randomSpecies = r.nextInt(population.size());
            population.add(mutation(population.get(randomSpecies)));
        }

        // insert crossovers
        for(int i = 0; i<numCrossover; i++){
            int randomSpecies1 = r.nextInt(population.size());
            int randomSpecies2 = r.nextInt(population.size());
            while(randomSpecies2==randomSpecies1){
                randomSpecies2 = r.nextInt(population.size());
            }
            double[] species1 = population.get(randomSpecies1);
            double[] species2 = population.get(randomSpecies2);
            population.add(crossOver(species1, species2));
        }
    }


    private double rangeTime(int paramIndex, double v1 , double v2)
    {
        double res = v1*v2 ;
        if(res>maxBound[paramIndex])
            res=maxBound[paramIndex];
        else if(res<minBound[paramIndex])
            res=minBound[paramIndex];
        return res;
    }
    private double[] mutation(double[] fenotype) {
        Random r = new Random();
        double rangeMin = 1 - mutationStrength;
        double rangeMax = 1 + mutationStrength;
        double randomValue;

        double[] newFenotype = fenotype.clone();

        for(int i=0;i<fenotype.length;i++){
            randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
            newFenotype[i] = rangeTime(i,fenotype[i], randomValue);
        }
        return newFenotype;
    }

    private double[] crossOver(double[] fenotype1, double[] fenotype2) {
        Random r = new Random();

        // deep copy
        double[] newFenotype = fenotype1.clone();
        int crossPoint=1;
        if(fenotype1.length > 2)
            crossPoint = r.nextInt(fenotype1.length-2)+1;

        for(int i=0;i<crossPoint;i++){
            newFenotype[i] = fenotype2[i];
        }
        return newFenotype;
    }
}
