package grocerykart;

import java.io.Serializable;
import java.util.ArrayList;


/*
 * This class consists of a path for a given truck e.g. 3-4-2-1-9-12-10-20
 * Stores using an arraylist of integers.
 */
class Route implements Serializable{

    public ArrayList<Integer> route;
    public double routeFitness;

    public Route() {
        route = new ArrayList<>();
    }
}

/*
 * Candidate class contains a chromosome of length 600
 * a fitness value of chromosome evaluated using total distance travelled and number of routes obtained in the chromosome.
 * rank value determined using pareto Ranking.
 * and an arraylist of class Route containing the path of each truck. The size of arraylist is number of trucks.
 */
class Candidate implements Comparable<Candidate>, Serializable {

    ArrayList<Integer> chromosome;
    double fitness;
    int rank;
    ArrayList<Route> routeTable;

    public Candidate() {
        chromosome = new ArrayList<>();
        routeTable = new ArrayList<>();
    }

    /*
     * toString() method overriden to get a nice formatted output of a given candidate
     */
    @Override
    public String toString() {
        int[] tmp = new int[chromosome.size()];
        String ret = "";
        for (Integer i : chromosome) {
            ret += (i + " ");
        }
        return ret;
    }

    @Override
    public int compareTo(Candidate tmp) {

        if(this.fitness < tmp.fitness) {
            return -1;
        } else if(this.fitness == tmp.fitness) {
            return 0;
        } else {
            return 1;
        }
        
//        if(this.rank < tmp.rank) {
//            return -1;
//        } else if(this.rank == tmp.rank) {
//            return 0;
//        } else {
//            return 1;
//        }
//        
        
    }
}