package com.abc.controller;

import com.abc.domian.Vehicle;
import com.abc.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/vehicle", produces = "application/json;charset=UTF-8")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    /*
     * ==========================================================
     * ENDPOINT LAMA
     * Tetap dipertahankan agar UI lama tetap bisa berjalan.
     * ==========================================================
     */

    @GetMapping
    public Result insertAll() {
        int i = vehicleService.insertAll();
        int code = (i == 1) ? Code.SAVE_OK : Code.SAVE_ERR;

        return new Result(i, code);
    }

    @PostMapping
    public Result selectByName(@RequestBody Vehicle vehicle) {
        int i = vehicleService.selectByName(vehicle.getName());
        int code = (i == 1) ? Code.GET_OK : Code.GET_ERR;

        return new Result(i, code);
    }

    @PutMapping
    public Result sendmessg(@RequestBody Vehicle vehicle) {
        int i = vehicleService.sendmessg(vehicle.getName(), vehicle.getPk1());
        int code = (i == 1) ? Code.UPDATE_OK : Code.UPDATE_ERR;

        return new Result(i, code);
    }

    /*
     * ==========================================================
     * CBAS-QIAO ENDPOINTS
     * ==========================================================
     */

    @GetMapping("/sign-qiao")
    public Result signQiao() {
        long start = System.nanoTime();

        int i = vehicleService.signQiao();

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.SAVE_OK : Code.SAVE_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-Qiao",
                "Sign-Qiao",
                i,
                elapsedMs
        );

        String message = (i == 1)
                ? "Qiao Sign success"
                : "Qiao Sign failed";

        return new Result(data, code, message);
    }

    @PostMapping("/verify-qiao")
    public Result verifyQiao(@RequestBody Vehicle vehicle) {
        long start = System.nanoTime();

        int i = vehicleService.verifyQiao(vehicle.getName());

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.GET_OK : Code.GET_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-Qiao",
                "Verify-Qiao",
                i,
                elapsedMs
        );

        String message = (i == 1)
                ? "Qiao Verify success"
                : "Qiao Verify failed";

        return new Result(data, code, message);
    }

    @GetMapping("/signagg-qiao")
    public Result signAggQiao() {
        long start = System.nanoTime();

        int n = 10;
        int i = vehicleService.signAggQiao(n);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.SAVE_OK : Code.SAVE_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-Qiao",
                "SignAgg-Qiao",
                i,
                elapsedMs
        );
        data.put("numberOfSignatures", n);

        String message = (i == 1)
                ? "Qiao aggregate signing success"
                : "Qiao aggregate signing failed";

        return new Result(data, code, message);
    }

    @GetMapping("/aggverify-qiao")
    public Result aggVerifyQiao() {
        long start = System.nanoTime();

        int n = 10;
        int i = vehicleService.aggVerifyQiao(n);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.GET_OK : Code.GET_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-Qiao",
                "AggVerify-Qiao",
                i,
                elapsedMs
        );
        data.put("numberOfSignatures", n);

        String message = (i == 1)
                ? "Qiao aggregate verification success"
                : "Qiao aggregate verification failed";

        return new Result(data, code, message);
    }

    /*
     * ==========================================================
     * CBAS-DSH ENDPOINTS
     * ==========================================================
     */

    @GetMapping("/sign-dsh")
    public Result signDsh() {
        long start = System.nanoTime();

        int i = vehicleService.signDsh();

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.SAVE_OK : Code.SAVE_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-DSH",
                "Sign-DSH",
                i,
                elapsedMs
        );

        String message = (i == 1)
                ? "CBAS-DSH Sign success"
                : "CBAS-DSH Sign failed";

        return new Result(data, code, message);
    }

    @PostMapping("/verify-dsh")
    public Result verifyDsh(@RequestBody Vehicle vehicle) {
        long start = System.nanoTime();

        int i = vehicleService.verifyDsh(vehicle.getName());

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.GET_OK : Code.GET_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-DSH",
                "Verify-DSH",
                i,
                elapsedMs
        );

        String message = (i == 1)
                ? "CBAS-DSH Verify success"
                : "CBAS-DSH Verify failed";

        return new Result(data, code, message);
    }

    @GetMapping("/signagg-dsh")
    public Result signAggDsh() {
        long start = System.nanoTime();

        int n = 10;
        int i = vehicleService.signAggDsh(n);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.SAVE_OK : Code.SAVE_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-DSH",
                "SignAgg-DSH",
                i,
                elapsedMs
        );
        data.put("numberOfSignatures", n);

        String message = (i == 1)
                ? "CBAS-DSH aggregate signing success"
                : "CBAS-DSH aggregate signing failed";

        return new Result(data, code, message);
    }

    @GetMapping("/aggverify-dsh")
    public Result aggVerifyDsh() {
        long start = System.nanoTime();

        int n = 10;
        int i = vehicleService.aggVerifyDsh(n);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int code = (i == 1) ? Code.GET_OK : Code.GET_ERR;

        Map<String, Object> data = buildBenchmarkData(
                "CBAS-DSH",
                "AggVerify-DSH",
                i,
                elapsedMs
        );
        data.put("numberOfSignatures", n);

        String message = (i == 1)
                ? "CBAS-DSH aggregate verification success"
                : "CBAS-DSH aggregate verification failed";

        return new Result(data, code, message);
    }

    /*
     * ==========================================================
     * RESPONSE HELPER
     * ==========================================================
     */

    private Map<String, Object> buildBenchmarkData(
            String scheme,
            String operation,
            int result,
            long elapsedMs
    ) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("scheme", scheme);
        data.put("operation", operation);
        data.put("result", result);
        data.put("valid", result == 1);
        data.put("controllerTimeMs", elapsedMs);

        return data;
    }
}