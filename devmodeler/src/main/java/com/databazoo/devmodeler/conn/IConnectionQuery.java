package com.databazoo.devmodeler.conn;

import java.io.Serializable;

public interface IConnectionQuery extends Serializable {
    ConnectionBase.Query run() throws DBCommException;

    Result fetchResult() throws DBCommException;

    void cancel();

    void commit() throws DBCommException;

    void rollback() throws DBCommException;

    void checkWarnings();

    boolean next();

    void close();

    String getRunningTimeString();

    float getTime();

    String getTimeString();

    String getWarnings();

    String getString(int i);

    String getString(String col);

    int getInt(int i);

    int getInt(String col);

    long getLong(int i);

    long getLong(String col);

    double getDouble(int i);

    double getDouble(String col);

    boolean getBool(int i);

    boolean getBool(String col);

    int[] getIntVector(int i);

    int[] getIntVector(String col);

    int[] getIntArray(int i);

    int[] getIntArray(String col);

    String[] getStringArray(int i);

    String[] getStringArray(String col);

    void useExecuteUpdate(boolean b);
}
