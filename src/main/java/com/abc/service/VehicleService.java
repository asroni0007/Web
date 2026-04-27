package com.abc.service;

public interface VehicleService {

    /*
     * Method lama agar controller lama tetap berjalan:
     * GET    /vehicle -> insertAll()
     * POST   /vehicle -> selectByName(...)
     * PUT    /vehicle -> sendmessg(...)
     */
    int insertAll();

    int selectByName(String name);

    int sendmessg(String name, String message);


    /*
     * CBAS-Qiao operations
     */
    int signQiao();

    int verifyQiao(String name);

    int signAggQiao(int n);

    int aggVerifyQiao(int n);


    /*
     * CBAS-DSH operations
     */
    int signDsh();

    int verifyDsh(String name);

    int signAggDsh(int n);

    int aggVerifyDsh(int n);
}