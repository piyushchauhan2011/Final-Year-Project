package grocerykart;

import java.io.*;
import java.util.*;

public class ReadData {

    GroceryKart g;

    public ReadData(GroceryKart g) {
        this.g = g;
        g.populationSize = 200;
        g.maxStops = 20;
        g.maxDistanceTravelled = 65;
        g.maxDemand = 1000;
        g.maxTime = 3;
    }

    public ReadData(GroceryKart g, boolean read) {
        this.g = g;
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter population size:");
        g.populationSize = sc.nextInt();
        System.out.println("Enter max no. of stops:");
        g.maxStops = sc.nextInt();
        System.out.println("Enter max distance permitted for each truck:");
        g.maxDistanceTravelled = sc.nextDouble();
        System.out.println("Enter max load capacity:");
        g.maxDemand = sc.nextInt();
        System.out.println("Enter max time:");
        g.maxTime = sc.nextInt();
    }

    public void readDistanceMatrix(FileReader fr) {
        try {
            //FileReader fr = new FileReader("DistanceMatrix.txt");
            Scanner in;
            in = new Scanner(fr);
            String tmp = in.next();
            while (tmp.compareTo("DC") != 0) {
                tmp = in.next();
            }
            g.DCStoreMatrix = new double[g.storeSize][g.storeSize];
            for (int i = 0; i < g.storeSize; i++) {
                in.next();
                for (int j = 0; j < g.storeSize; j++) {
                    tmp = in.next();
                    if (tmp.compareTo("-") != 0) {
                        g.DCStoreMatrix[i][j] = Double.parseDouble(tmp);
                    } else {
                        g.DCStoreMatrix[i][j] = -1;
                    }
                }
            }
            in.close();
        } catch (Exception e) {
        }
    }

    public void displayDistanceMatrix() {
        // Printing values for checking
        System.out.println(g.stores.get(g.storeSize - 1)); // storeSize-1 denotes the index of Warehouse or Data centre row.
        System.out.println("Some Values are: ");
        for (int j = 0; j < g.storeSize; j++) {
            System.out.print(g.DCStoreMatrix[g.storeSize - 1][j] + " ");
        }
        System.out.println("");
    }

    public void readDemandTable(FileReader fr) {
        try {
            //FileReader fr = new FileReader("DemandTable.txt");
            Scanner in;
            in = new Scanner(fr);
            int k = 0;
            Store tmp;

            tmp = new Store();
            tmp.storeName = "DC";
            tmp.storeId = k;
            tmp.demand = 0;
            k++;
            g.stores.add(tmp);

            while (in.hasNext()) {
                tmp = new Store();
                tmp.storeName = in.next();
                tmp.storeId = k;
                tmp.demand = Integer.parseInt(in.next());
                k++;
                g.stores.add(tmp);
            }
            g.storeSize = g.stores.size();
            in.close();
        } catch (Exception e) {
        }
    }

    public void displayDemandTable() {
        // Printing values for checking.
        System.out.println("Demand Table: ");
        System.out.println("Store Size: " + g.storeSize);
        for (int i = 0; i < g.storeSize; i++) {
            System.out.println(g.stores.get(i).toString());
        }
    }
}
