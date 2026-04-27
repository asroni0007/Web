package com.abc.domian;

import it.unisa.dia.gas.jpbc.Element;

public class ms {
    private Element B;
    private Element A;
    private Element sigma;
    private Element pk1;
    private Element pk2;

    private Element pub;
    private Element y;

    private Element P;

    public Element getP() {
        return P;
    }

    public void setP(Element p) {
        P = p;
    }

    public ms(Element b, Element a, Element sigma, Element pk1, Element pk2, Element pub, Element y, Element p) {
        B = b;
        A = a;
        this.sigma = sigma;
        this.pk1 = pk1;
        this.pk2 = pk2;
        this.pub = pub;
        this.y = y;
        P = p;
    }

    public Element getY() {
        return y;
    }

    public void setY(Element y) {
        this.y = y;
    }

    public ms(Element b, Element a, Element sigma, Element pk1, Element pk2, Element pub, Element y) {
        B = b;
        A = a;
        this.sigma = sigma;
        this.pk1 = pk1;
        this.pk2 = pk2;
        this.pub = pub;
        this.y = y;
    }

    public Element getPub() {
        return pub;
    }

    public void setPub(Element pub) {
        this.pub = pub;
    }

    public ms(Element b, Element a, Element sigma, Element pk1, Element pk2, Element pub) {
        B = b;
        A = a;
        this.sigma = sigma;
        this.pk1 = pk1;
        this.pk2 = pk2;
        this.pub = pub;
    }

    public Element getPk1() {
        return pk1;
    }

    public void setPk1(Element pk1) {
        this.pk1 = pk1;
    }

    public Element getPk2() {
        return pk2;
    }

    public void setPk2(Element pk2) {
        this.pk2 = pk2;
    }

    public ms(Element b, Element a, Element sigma, Element pk1, Element pk2) {
        B = b;
        A = a;
        this.sigma = sigma;
        this.pk1 = pk1;
        this.pk2 = pk2;
    }

    public Element getB() {
        return B;
    }

    public void setB(Element b) {
        B = b;
    }

    public Element getA() {
        return A;
    }

    public void setA(Element a) {
        A = a;
    }

    public Element getSigma() {
        return sigma;
    }

    public void setSigma(Element sigma) {
        this.sigma = sigma;
    }

    public ms() {
    }

    public ms(Element b, Element a, Element sigma) {
        B = b;
        A = a;
        this.sigma = sigma;
    }
}
