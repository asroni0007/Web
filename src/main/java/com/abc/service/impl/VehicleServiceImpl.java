package com.abc.service.impl;

import com.abc.service.VehicleService;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final int R_BITS = 160;
    private static final int Q_BITS = 512;

    private static final String TAG_CERT = "CBAS-DSH-CERT";
    private static final String TAG_SIG = "CBAS-DSH-SIG";

    /*
     * ==========================================================
     * METHOD LAMA
     * ==========================================================
     *
     * Method ini tetap dipertahankan agar controller lama tetap jalan.
     *
     * insertAll()        -> diarahkan ke signQiao()
     * selectByName()     -> diarahkan ke verifyQiao()
     * sendmessg()        -> diarahkan ke verifyDsh()
     */

    @Override
    public int insertAll() {
        return signQiao();
    }

    @Override
    public int selectByName(String name) {
        return verifyQiao(name);
    }

    @Override
    public int sendmessg(String name, String message) {
        String id = safeText(name, "vehicle-01");
        String msg = safeText(message, "default-message");

        return verifyDsh(id + "-" + msg);
    }


    /*
     * ==========================================================
     * CBAS-QIAO
     * ==========================================================
     *
     * Persamaan utama:
     *
     * Ppub = sP
     * Ri   = riP
     * h1i  = H1(IDi, Ri, Ppub)
     * di   = ri + s h1i
     * Xi   = xiP
     * Ui   = uiP
     * h2i  = H2(Mi, PKi, Ui)
     * h3i  = H3(Ppub, PKi, Ui, h2i)
     * h4i  = H4(Ppub, PKi, Ui, h2i)
     *
     * delta_i = h2i di + h3i ui + h4i xi
     *
     * Verify:
     *
     * delta_i P = h2i(Ri + h1i Ppub) + h3i Ui + h4i Xi
     */

    @Override
    public int signQiao() {
        SchemeContext ctx = setupContext();

        QiaoSignature signature = createQiaoSignature(
                ctx,
                "vehicle-qiao",
                "message-qiao"
        );

        return signature != null ? 1 : 0;
    }

    @Override
    public int verifyQiao(String name) {
        SchemeContext ctx = setupContext();

        String id = safeText(name, "vehicle-qiao");

        QiaoSignature signature = createQiaoSignature(
                ctx,
                id,
                "message-qiao"
        );

        return verifyQiaoSignature(ctx, signature) ? 1 : 0;
    }

    @Override
    public int signAggQiao(int n) {
        SchemeContext ctx = setupContext();

        List<QiaoSignature> signatures = createQiaoSignatures(ctx, n);

        return signatures.isEmpty() ? 0 : 1;
    }

    @Override
    public int aggVerifyQiao(int n) {
        SchemeContext ctx = setupContext();

        List<QiaoSignature> signatures = createQiaoSignatures(ctx, n);

        if (signatures.isEmpty()) {
            return 0;
        }

        Element delta = scalarZero(ctx);

        for (QiaoSignature signature : signatures) {
            delta = scalarAdd(ctx, delta, signature.delta);
        }

        return verifyQiaoAggregate(ctx, signatures, delta) ? 1 : 0;
    }


    /*
     * Membuat signature individual CBAS-Qiao.
     */
    private QiaoSignature createQiaoSignature(
            SchemeContext ctx,
            String id,
            String message
    ) {
        QiaoSignature sig = new QiaoSignature();

        sig.id = safeText(id, "vehicle-qiao");
        sig.message = safeText(message, "message-qiao");


        /*
         * CertGen:
         *
         * Ri = riP
         * h1i = H1(IDi, Ri, Ppub)
         * di = ri + s h1i
         */
        sig.ri = randomScalar(ctx);
        sig.Ri = pointMul(ctx.P, sig.ri);

        sig.h1 = hashToZr(
                ctx,
                "QIAO-H1",
                sig.id,
                sig.Ri,
                ctx.Ppub
        );

        sig.di = scalarAdd(
                ctx,
                sig.ri,
                scalarMul(ctx.s, sig.h1)
        );


        /*
         * Validasi sertifikat:
         *
         * diP = Ri + h1i Ppub
         */
        if (!verifyCertificate(ctx, sig.di, sig.Ri, sig.h1)) {
            return null;
        }


        /*
         * KeyGen:
         *
         * xi in Zq
         * Xi = xiP
         */
        sig.xi = randomScalar(ctx);
        sig.Xi = pointMul(ctx.P, sig.xi);


        /*
         * Sign:
         *
         * ui random
         * Ui = uiP
         */
        sig.ui = randomScalar(ctx);
        sig.Ui = pointMul(ctx.P, sig.ui);


        /*
         * Hash Qiao:
         *
         * h2i = H2(Mi, PKi, Ui)
         * h3i = H3(Ppub, PKi, Ui, h2i)
         * h4i = H4(Ppub, PKi, Ui, h2i)
         *
         * Dalam implementasi:
         * PKi direpresentasikan oleh pasangan (Ri, Xi).
         */
        sig.h2 = hashToZr(
                ctx,
                "QIAO-H2",
                sig.message,
                sig.Ri,
                sig.Xi,
                sig.Ui
        );

        sig.h3 = hashToZr(
                ctx,
                "QIAO-H3",
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                sig.h2
        );

        sig.h4 = hashToZr(
                ctx,
                "QIAO-H4",
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                sig.h2
        );


        /*
         * delta_i = h2i di + h3i ui + h4i xi
         */
        sig.delta = scalarAdd(
                ctx,
                scalarMul(sig.h2, sig.di),
                scalarMul(sig.h3, sig.ui),
                scalarMul(sig.h4, sig.xi)
        );

        return sig;
    }


    /*
     * Verify individual CBAS-Qiao:
     *
     * delta_i P =
     * h2i(Ri + h1i Ppub) + h3i Ui + h4i Xi
     */
    private boolean verifyQiaoSignature(
            SchemeContext ctx,
            QiaoSignature sig
    ) {
        if (sig == null) {
            return false;
        }

        Element h1 = hashToZr(
                ctx,
                "QIAO-H1",
                sig.id,
                sig.Ri,
                ctx.Ppub
        );

        Element h2 = hashToZr(
                ctx,
                "QIAO-H2",
                sig.message,
                sig.Ri,
                sig.Xi,
                sig.Ui
        );

        Element h3 = hashToZr(
                ctx,
                "QIAO-H3",
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                h2
        );

        Element h4 = hashToZr(
                ctx,
                "QIAO-H4",
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                h2
        );

        Element left = pointMul(ctx.P, sig.delta);

        Element certificatePart = pointAdd(
                ctx,
                sig.Ri,
                pointMul(ctx.Ppub, h1)
        );

        Element right = pointAdd(
                ctx,
                pointMul(certificatePart, h2),
                pointMul(sig.Ui, h3),
                pointMul(sig.Xi, h4)
        );

        return left.isEqual(right);
    }


    private List<QiaoSignature> createQiaoSignatures(
            SchemeContext ctx,
            int n
    ) {
        int size = Math.max(1, n);

        List<QiaoSignature> signatures = new ArrayList<>();

        for (int i = 1; i <= size; i++) {
            QiaoSignature signature = createQiaoSignature(
                    ctx,
                    "vehicle-qiao-" + i,
                    "message-qiao-" + i
            );

            if (signature != null) {
                signatures.add(signature);
            }
        }

        return signatures;
    }


    /*
     * AggVerify Qiao:
     *
     * delta P =
     * sum h2i Ri
     * + sum h2i h1i Ppub
     * + sum h3i Ui
     * + sum h4i Xi
     */
    private boolean verifyQiaoAggregate(
            SchemeContext ctx,
            List<QiaoSignature> signatures,
            Element aggregateDelta
    ) {
        Element left = pointMul(ctx.P, aggregateDelta);

        Element right = pointZero(ctx);

        for (QiaoSignature sig : signatures) {
            Element h1 = hashToZr(
                    ctx,
                    "QIAO-H1",
                    sig.id,
                    sig.Ri,
                    ctx.Ppub
            );

            Element h2 = hashToZr(
                    ctx,
                    "QIAO-H2",
                    sig.message,
                    sig.Ri,
                    sig.Xi,
                    sig.Ui
            );

            Element h3 = hashToZr(
                    ctx,
                    "QIAO-H3",
                    ctx.Ppub,
                    sig.Ri,
                    sig.Xi,
                    sig.Ui,
                    h2
            );

            Element h4 = hashToZr(
                    ctx,
                    "QIAO-H4",
                    ctx.Ppub,
                    sig.Ri,
                    sig.Xi,
                    sig.Ui,
                    h2
            );

            right = pointAdd(
                    ctx,
                    right,
                    pointMul(sig.Ri, h2),
                    pointMul(ctx.Ppub, scalarMul(h2, h1)),
                    pointMul(sig.Ui, h3),
                    pointMul(sig.Xi, h4)
            );
        }

        return left.isEqual(right);
    }


    /*
     * ==========================================================
     * CBAS-DSH
     * ==========================================================
     *
     * Penguatan:
     *
     * 1. Domain-separated hash:
     *    tag_cert dan tag_sig
     *
     * 2. Proof-of-Possession:
     *    ci = H(tag_cert || IDi || Xi || Wi)
     *    zi = wi + ci xi
     *    ziP = Wi + ciXi
     *
     * 3. Deterministic nonce:
     *    ui = H_N(tag_sig || IDi || Mi || xi)
     */

    @Override
    public int signDsh() {
        SchemeContext ctx = setupContext();

        DshSignature signature = createDshSignature(
                ctx,
                "vehicle-dsh",
                "message-dsh"
        );

        return signature != null ? 1 : 0;
    }

    @Override
    public int verifyDsh(String name) {
        SchemeContext ctx = setupContext();

        String id = safeText(name, "vehicle-dsh");

        DshSignature signature = createDshSignature(
                ctx,
                id,
                "message-dsh"
        );

        return verifyDshSignature(ctx, signature) ? 1 : 0;
    }

    @Override
    public int signAggDsh(int n) {
        SchemeContext ctx = setupContext();

        List<DshSignature> signatures = createDshSignatures(ctx, n);

        return signatures.isEmpty() ? 0 : 1;
    }

    @Override
    public int aggVerifyDsh(int n) {
        SchemeContext ctx = setupContext();

        List<DshSignature> signatures = createDshSignatures(ctx, n);

        if (signatures.isEmpty()) {
            return 0;
        }

        Element delta = scalarZero(ctx);

        for (DshSignature signature : signatures) {
            delta = scalarAdd(ctx, delta, signature.delta);
        }

        return verifyDshAggregate(ctx, signatures, delta) ? 1 : 0;
    }


    /*
     * Membuat signature individual CBAS-DSH.
     */
    private DshSignature createDshSignature(
            SchemeContext ctx,
            String id,
            String message
    ) {
        DshSignature sig = new DshSignature();

        sig.id = safeText(id, "vehicle-dsh");
        sig.message = safeText(message, "message-dsh");


        /*
         * KeyGen:
         *
         * xi in Zq
         * Xi = xiP
         */
        sig.xi = randomScalar(ctx);
        sig.Xi = pointMul(ctx.P, sig.xi);


        /*
         * Proof-of-Possession:
         *
         * wi random
         * Wi = wiP
         * ci = H(tag_cert || IDi || Xi || Wi)
         * zi = wi + ci xi
         *
         * Verify PoP:
         *
         * ziP = Wi + ciXi
         */
        Element wi = randomScalar(ctx);
        Element Wi = pointMul(ctx.P, wi);

        Element ci = hashToZr(
                ctx,
                "DSH-POP",
                TAG_CERT,
                sig.id,
                sig.Xi,
                Wi
        );

        Element zi = scalarAdd(
                ctx,
                wi,
                scalarMul(ci, sig.xi)
        );

        Element popLeft = pointMul(ctx.P, zi);

        Element popRight = pointAdd(
                ctx,
                Wi,
                pointMul(sig.Xi, ci)
        );

        if (!popLeft.isEqual(popRight)) {
            return null;
        }


        /*
         * CertGen:
         *
         * Ri = riP
         * h1 = H(tag_cert || IDi || Ri || Ppub)
         * di = ri + s h1
         */
        sig.ri = randomScalar(ctx);
        sig.Ri = pointMul(ctx.P, sig.ri);

        sig.h1 = hashToZr(
                ctx,
                "DSH-H1",
                TAG_CERT,
                sig.id,
                sig.Ri,
                ctx.Ppub
        );

        sig.di = scalarAdd(
                ctx,
                sig.ri,
                scalarMul(ctx.s, sig.h1)
        );


        /*
         * Validasi sertifikat:
         *
         * diP = Ri + h1 Ppub
         */
        if (!verifyCertificate(ctx, sig.di, sig.Ri, sig.h1)) {
            return null;
        }


        /*
         * Deterministic nonce:
         *
         * ui = H_N(tag_sig || IDi || Mi || xi)
         * Ui = uiP
         */
        sig.ui = hashToZr(
                ctx,
                "DSH-HN",
                TAG_SIG,
                sig.id,
                sig.message,
                sig.xi
        );

        sig.Ui = pointMul(ctx.P, sig.ui);


        /*
         * Domain-separated hash values:
         *
         * h2 = H(tag_sig || Mi || Ri || Xi || Ui)
         * h3 = H(tag_sig || Ppub || Ri || Xi || Ui || h2)
         * h4 = H(tag_sig || Ppub || Ri || Xi || Ui || h2)
         */
        sig.h2 = hashToZr(
                ctx,
                "DSH-H2",
                TAG_SIG,
                sig.message,
                sig.Ri,
                sig.Xi,
                sig.Ui
        );

        sig.h3 = hashToZr(
                ctx,
                "DSH-H3",
                TAG_SIG,
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                sig.h2
        );

        sig.h4 = hashToZr(
                ctx,
                "DSH-H4",
                TAG_SIG,
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                sig.h2
        );


        /*
         * delta_i = h2i di + h3i ui + h4i xi
         */
        sig.delta = scalarAdd(
                ctx,
                scalarMul(sig.h2, sig.di),
                scalarMul(sig.h3, sig.ui),
                scalarMul(sig.h4, sig.xi)
        );

        return sig;
    }


    /*
     * Verify individual CBAS-DSH:
     *
     * delta_i P =
     * h2i(Ri + h1i Ppub) + h3i Ui + h4i Xi
     */
    private boolean verifyDshSignature(
            SchemeContext ctx,
            DshSignature sig
    ) {
        if (sig == null) {
            return false;
        }

        Element h1 = hashToZr(
                ctx,
                "DSH-H1",
                TAG_CERT,
                sig.id,
                sig.Ri,
                ctx.Ppub
        );

        Element h2 = hashToZr(
                ctx,
                "DSH-H2",
                TAG_SIG,
                sig.message,
                sig.Ri,
                sig.Xi,
                sig.Ui
        );

        Element h3 = hashToZr(
                ctx,
                "DSH-H3",
                TAG_SIG,
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                h2
        );

        Element h4 = hashToZr(
                ctx,
                "DSH-H4",
                TAG_SIG,
                ctx.Ppub,
                sig.Ri,
                sig.Xi,
                sig.Ui,
                h2
        );

        Element left = pointMul(ctx.P, sig.delta);

        Element certificatePart = pointAdd(
                ctx,
                sig.Ri,
                pointMul(ctx.Ppub, h1)
        );

        Element right = pointAdd(
                ctx,
                pointMul(certificatePart, h2),
                pointMul(sig.Ui, h3),
                pointMul(sig.Xi, h4)
        );

        return left.isEqual(right);
    }


    private List<DshSignature> createDshSignatures(
            SchemeContext ctx,
            int n
    ) {
        int size = Math.max(1, n);

        List<DshSignature> signatures = new ArrayList<>();

        for (int i = 1; i <= size; i++) {
            DshSignature signature = createDshSignature(
                    ctx,
                    "vehicle-dsh-" + i,
                    "message-dsh-" + i
            );

            if (signature != null) {
                signatures.add(signature);
            }
        }

        return signatures;
    }


    /*
     * AggVerify CBAS-DSH:
     *
     * delta P =
     * sum h2i Ri
     * + sum h2i h1i Ppub
     * + sum h3i Ui
     * + sum h4i Xi
     */
    private boolean verifyDshAggregate(
            SchemeContext ctx,
            List<DshSignature> signatures,
            Element aggregateDelta
    ) {
        Element left = pointMul(ctx.P, aggregateDelta);

        Element right = pointZero(ctx);

        for (DshSignature sig : signatures) {
            Element h1 = hashToZr(
                    ctx,
                    "DSH-H1",
                    TAG_CERT,
                    sig.id,
                    sig.Ri,
                    ctx.Ppub
            );

            Element h2 = hashToZr(
                    ctx,
                    "DSH-H2",
                    TAG_SIG,
                    sig.message,
                    sig.Ri,
                    sig.Xi,
                    sig.Ui
            );

            Element h3 = hashToZr(
                    ctx,
                    "DSH-H3",
                    TAG_SIG,
                    ctx.Ppub,
                    sig.Ri,
                    sig.Xi,
                    sig.Ui,
                    h2
            );

            Element h4 = hashToZr(
                    ctx,
                    "DSH-H4",
                    TAG_SIG,
                    ctx.Ppub,
                    sig.Ri,
                    sig.Xi,
                    sig.Ui,
                    h2
            );

            right = pointAdd(
                    ctx,
                    right,
                    pointMul(sig.Ri, h2),
                    pointMul(ctx.Ppub, scalarMul(h2, h1)),
                    pointMul(sig.Ui, h3),
                    pointMul(sig.Xi, h4)
            );
        }

        return left.isEqual(right);
    }


    /*
     * ==========================================================
     * COMMON SETUP AND HELPER METHODS
     * ==========================================================
     */

    private SchemeContext setupContext() {
        TypeACurveGenerator generator = new TypeACurveGenerator(
                R_BITS,
                Q_BITS
        );

        PairingParameters params = generator.generate();

        Pairing pairing = PairingFactory.getPairing(params);

        SchemeContext ctx = new SchemeContext();

        ctx.pairing = pairing;
        ctx.G1 = pairing.getG1();
        ctx.Zq = pairing.getZr();

        ctx.P = ctx.G1.newRandomElement().getImmutable();

        ctx.s = ctx.Zq.newRandomElement().getImmutable();

        ctx.Ppub = pointMul(ctx.P, ctx.s);

        return ctx;
    }


    /*
     * Certificate verification:
     *
     * diP = Ri + h1 Ppub
     */
    private boolean verifyCertificate(
            SchemeContext ctx,
            Element di,
            Element Ri,
            Element h1
    ) {
        Element left = pointMul(ctx.P, di);

        Element right = pointAdd(
                ctx,
                Ri,
                pointMul(ctx.Ppub, h1)
        );

        return left.isEqual(right);
    }


    private Element randomScalar(SchemeContext ctx) {
        return ctx.Zq.newRandomElement().getImmutable();
    }


    private Element scalarZero(SchemeContext ctx) {
        return ctx.Zq.newZeroElement().getImmutable();
    }


    private Element pointZero(SchemeContext ctx) {
        return ctx.G1.newZeroElement().getImmutable();
    }


    private Element scalarAdd(
            SchemeContext ctx,
            Element... terms
    ) {
        Element acc = ctx.Zq.newZeroElement();

        if (terms != null) {
            for (Element term : terms) {
                if (term != null) {
                    acc.add(term);
                }
            }
        }

        return acc.getImmutable();
    }


    private Element scalarMul(
            Element a,
            Element b
    ) {
        return a.duplicate().mul(b).getImmutable();
    }


    private Element pointAdd(
            SchemeContext ctx,
            Element... terms
    ) {
        Element acc = ctx.G1.newZeroElement();

        if (terms != null) {
            for (Element term : terms) {
                if (term != null) {
                    acc.add(term);
                }
            }
        }

        return acc.getImmutable();
    }


    private Element pointMul(
            Element point,
            Element scalar
    ) {
        return point.duplicate().mulZn(scalar).getImmutable();
    }


    /*
     * Hash-to-Zr dengan SHA-256.
     *
     * Setiap input diberi prefix panjang bytes agar aman dari ambiguity:
     *
     * H("ab", "c") tidak sama dengan H("a", "bc")
     */
    private Element hashToZr(
            SchemeContext ctx,
            Object... parts
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            if (parts != null) {
                for (Object part : parts) {
                    byte[] bytes = toBytes(part);

                    digest.update(
                            ByteBuffer
                                    .allocate(4)
                                    .putInt(bytes.length)
                                    .array()
                    );

                    digest.update(bytes);
                }
            }

            byte[] hash = digest.digest();

            return ctx.Zq
                    .newElement()
                    .setFromHash(hash, 0, hash.length)
                    .getImmutable();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }


    private byte[] toBytes(Object part) {
        if (part == null) {
            return new byte[0];
        }

        if (part instanceof Element) {
            return ((Element) part).toBytes();
        }

        if (part instanceof String) {
            return ((String) part).getBytes(StandardCharsets.UTF_8);
        }

        if (part instanceof Integer) {
            return ByteBuffer
                    .allocate(4)
                    .putInt((Integer) part)
                    .array();
        }

        if (part instanceof Long) {
            return ByteBuffer
                    .allocate(8)
                    .putLong((Long) part)
                    .array();
        }

        return String
                .valueOf(part)
                .getBytes(StandardCharsets.UTF_8);
    }


    private String safeText(
            String value,
            String defaultValue
    ) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }


    /*
     * ==========================================================
     * INTERNAL DATA STRUCTURES
     * ==========================================================
     */

    private static class SchemeContext {
        private Pairing pairing;
        private Field G1;
        private Field Zq;

        private Element P;
        private Element s;
        private Element Ppub;
    }


    private static class QiaoSignature {
        private String id;
        private String message;

        private Element ri;
        private Element di;
        private Element Ri;

        private Element xi;
        private Element Xi;

        private Element ui;
        private Element Ui;

        private Element h1;
        private Element h2;
        private Element h3;
        private Element h4;

        private Element delta;
    }


    private static class DshSignature {
        private String id;
        private String message;

        private Element ri;
        private Element di;
        private Element Ri;

        private Element xi;
        private Element Xi;

        private Element ui;
        private Element Ui;

        private Element h1;
        private Element h2;
        private Element h3;
        private Element h4;

        private Element delta;
    }
}
