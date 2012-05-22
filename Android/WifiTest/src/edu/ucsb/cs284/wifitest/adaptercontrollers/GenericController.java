package edu.ucsb.cs284.wifitest.adaptercontrollers;

public class GenericController implements AdapterController {

    @Override
    public boolean setTxPower(int dbm) {
        // $ su -c "iwconfig eth0 txpower [dbm]"
        throw new UnsupportedOperationException("TODO");
    }

}
