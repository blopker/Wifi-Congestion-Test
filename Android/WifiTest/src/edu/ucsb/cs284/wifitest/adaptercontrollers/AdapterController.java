package edu.ucsb.cs284.wifitest.adaptercontrollers;

/**
 * Unfortunately, different devices have different methods for setting transmit power.
 *
 */
public interface AdapterController {
    boolean setTxPower(int dbm);
}
