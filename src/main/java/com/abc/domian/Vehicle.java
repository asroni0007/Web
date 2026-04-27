package com.abc.domian;

public class Vehicle {
    private int id;
    private String name;
    private String pk1;
    private String pk2;
    private String sk1;
    private String sk2;

    public Vehicle() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPk1() {
        return pk1;
    }

    public void setPk1(String pk1) {
        this.pk1 = pk1;
    }

    public String getPk2() {
        return pk2;
    }

    public void setPk2(String pk2) {
        this.pk2 = pk2;
    }

    public String getSk1() {
        return sk1;
    }

    public void setSk1(String sk1) {
        this.sk1 = sk1;
    }

    public String getSk2() {
        return sk2;
    }

    public void setSk2(String sk2) {
        this.sk2 = sk2;
    }

    public Vehicle(int id, String name, String pk1, String pk2, String sk1, String sk2) {
        this.id = id;
        this.name = name;
        this.pk1 = pk1;
        this.pk2 = pk2;
        this.sk1 = sk1;
        this.sk2 = sk2;
    }
}
