
package grocerykart;

import java.io.Serializable;

public class Store implements Serializable {
    
    int storeId;
    String storeName;
    int demand;

    @Override
    public String toString() {
        String ret = "Store Id: " + storeId + "\t";
        ret += "Store Name: " + storeName + "\t";
        ret += "Demand: " + demand;
        return ret;
    }
    
}
