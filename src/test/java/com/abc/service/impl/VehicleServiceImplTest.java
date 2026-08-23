package com.abc.service.impl;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleServiceImplTest {

    private VehicleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VehicleServiceImpl();
    }

    @Test
    void publicIndividualOperationsCompleteSuccessfully() {
        assertEquals(1, service.signQiao());
        assertEquals(1, service.verifyQiao("vehicle-qiao-test"));
        assertEquals(1, service.signDsh());
        assertEquals(1, service.verifyDsh("vehicle-dsh-test"));
    }

    @Test
    void publicAggregateOperationsCompleteSuccessfully() {
        assertEquals(1, service.signAggQiao(3));
        assertEquals(1, service.aggVerifyQiao(3));
        assertEquals(1, service.signAggDsh(3));
        assertEquals(1, service.aggVerifyDsh(3));
    }

    @Test
    void qiaoVerificationRejectsModifiedMessage() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);
        Object signature = invoke(
                "createQiaoSignature",
                new Class<?>[]{context.getClass(), String.class, String.class},
                context, "vehicle-qiao", "original-message"
        );

        setField(signature, "message", "modified-message");

        boolean valid = (boolean) invoke(
                "verifyQiaoSignature",
                new Class<?>[]{context.getClass(), signature.getClass()},
                context, signature
        );
        assertFalse(valid);
    }

    @Test
    void dshVerificationRejectsModifiedMessage() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);
        Object signature = invoke(
                "createDshSignature",
                new Class<?>[]{context.getClass(), String.class, String.class},
                context, "vehicle-dsh", "original-message"
        );

        setField(signature, "message", "modified-message");

        boolean valid = (boolean) invoke(
                "verifyDshSignature",
                new Class<?>[]{context.getClass(), signature.getClass()},
                context, signature
        );
        assertFalse(valid);
    }

    @Test
    void dshVerificationRejectsModifiedSignatureScalar() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);
        Object signature = invoke(
                "createDshSignature",
                new Class<?>[]{context.getClass(), String.class, String.class},
                context, "vehicle-dsh", "message-dsh"
        );

        Field<?> scalarField = (Field<?>) getField(context, "Zq");
        Element modifiedDelta = scalarField.newRandomElement().getImmutable();
        setField(signature, "delta", modifiedDelta);

        boolean valid = (boolean) invoke(
                "verifyDshSignature",
                new Class<?>[]{context.getClass(), signature.getClass()},
                context, signature
        );
        assertFalse(valid);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aggregateVerificationRejectsMissingMember() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);
        List<Object> signatures = (List<Object>) invoke(
                "createDshSignatures",
                new Class<?>[]{context.getClass(), int.class},
                context, 3
        );

        Field<?> scalarField = (Field<?>) getField(context, "Zq");
        Element aggregateDelta = scalarField.newZeroElement();
        for (Object signature : signatures) {
            aggregateDelta.add((Element) getField(signature, "delta"));
        }
        aggregateDelta = aggregateDelta.getImmutable();

        signatures.remove(signatures.size() - 1);
        boolean valid = (boolean) invoke(
                "verifyDshAggregate",
                new Class<?>[]{context.getClass(), List.class, Element.class},
                context, signatures, aggregateDelta
        );
        assertFalse(valid);
    }

    @Test
    void hashEncodingIsDeterministicAndDomainSeparated() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);

        Element first = hash(context, "DSH-H2", "CBAS-DSH-SIG", "message");
        Element repeated = hash(context, "DSH-H2", "CBAS-DSH-SIG", "message");
        Element differentRole = hash(context, "DSH-H3", "CBAS-DSH-SIG", "message");
        Element differentDomain = hash(context, "DSH-H2", "CBAS-DSH-CERT", "message");

        assertTrue(first.isEqual(repeated));
        assertFalse(first.isEqual(differentRole));
        assertFalse(first.isEqual(differentDomain));
    }

    @Test
    void deterministicNonceMatchesSpecifiedDerivation() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);
        Object signature = invoke(
                "createDshSignature",
                new Class<?>[]{context.getClass(), String.class, String.class},
                context, "vehicle-dsh", "message-dsh"
        );

        Element expected = hash(
                context,
                "DSH-HN",
                "CBAS-DSH-SIG",
                getField(signature, "id"),
                getField(signature, "message"),
                getField(signature, "xi")
        );

        assertTrue(expected.isEqual((Element) getField(signature, "ui")));
    }

    @Test
    void proofOfPossessionRejectsModifiedResponse() throws Exception {
        Object context = invoke("setupContext", new Class<?>[0]);
        Field<?> scalarField = (Field<?>) getField(context, "Zq");
        Element generator = (Element) getField(context, "P");

        Element secret = scalarField.newRandomElement().getImmutable();
        Element witness = scalarField.newRandomElement().getImmutable();
        Element publicKey = multiplyPoint(generator, secret);
        Element commitment = multiplyPoint(generator, witness);
        Element challenge = hash(
                context,
                "DSH-POP",
                "CBAS-DSH-CERT",
                "vehicle-dsh",
                publicKey,
                commitment
        );
        Element response = witness.duplicate()
                .add(challenge.duplicate().mul(secret))
                .getImmutable();

        Class<?> contextClass = context.getClass();
        Class<?>[] types = {
                contextClass, Element.class, Element.class, Element.class, Element.class
        };
        assertTrue((boolean) invoke(
                "verifyProofOfPossession",
                types,
                context, publicKey, commitment, challenge, response
        ));

        Element modifiedResponse = response.duplicate()
                .add(scalarField.newOneElement())
                .getImmutable();
        assertFalse((boolean) invoke(
                "verifyProofOfPossession",
                types,
                context, publicKey, commitment, challenge, modifiedResponse
        ));
    }

    private Element hash(Object context, Object... parts) throws Exception {
        return (Element) invoke(
                "hashToZr",
                new Class<?>[]{context.getClass(), Object[].class},
                context, (Object) parts
        );
    }

    private Element multiplyPoint(Element point, Element scalar) {
        return point.duplicate().mulZn(scalar).getImmutable();
    }

    private Object invoke(String name, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = VehicleServiceImpl.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, arguments);
    }

    private static Object getField(Object target, String name) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
