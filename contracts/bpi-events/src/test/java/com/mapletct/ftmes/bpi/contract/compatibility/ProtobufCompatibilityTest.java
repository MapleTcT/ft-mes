package com.mapletct.ftmes.bpi.contract.compatibility;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.mapletct.ftmes.bpi.contract.v1.BpiEventsV1Proto;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProtobufCompatibilityTest {

    @Test
    public void v1FieldNumbersAndWireTypesMatchTheReviewedBaseline() throws Exception {
        List<String> expected = readBaseline("/compatibility/bpi-events-v1.fields");
        List<String> actual = describe(BpiEventsV1Proto.getDescriptor());

        assertEquals(
            "BPI v1 contract changed. Existing lines are immutable; compatible additions require review.",
            expected,
            actual
        );
    }

    @Test
    public void generatedDescriptorIsPublishedAsAClasspathArtifact() throws Exception {
        InputStream descriptor = ProtobufCompatibilityTest.class.getResourceAsStream(
            "/META-INF/protobuf/bpi-events-v1.desc"
        );

        assertNotNull("generated descriptor must be packaged for registry publication", descriptor);
        try {
            assertTrue("generated descriptor must not be empty", descriptor.read() >= 0);
        } finally {
            descriptor.close();
        }
    }

    private static List<String> describe(FileDescriptor file) {
        List<String> result = new ArrayList<String>();
        for (Descriptor message : file.getMessageTypes()) {
            for (FieldDescriptor field : message.getFields()) {
                result.add("M|" + message.getFullName()
                    + "|" + field.getName()
                    + "|" + field.getNumber()
                    + "|" + fieldType(field)
                    + "|" + cardinality(field)
                    + "|" + oneof(field));
            }
        }
        for (EnumDescriptor enumeration : file.getEnumTypes()) {
            for (EnumValueDescriptor value : enumeration.getValues()) {
                result.add("E|" + enumeration.getFullName()
                    + "|" + value.getName()
                    + "|" + value.getNumber());
            }
        }
        return result;
    }

    private static String fieldType(FieldDescriptor field) {
        if (field.getType() == FieldDescriptor.Type.MESSAGE
            && field.getMessageType().getOptions().getMapEntry()) {
            FieldDescriptor key = field.getMessageType().findFieldByName("key");
            FieldDescriptor value = field.getMessageType().findFieldByName("value");
            return "MAP<" + key.getType().name() + "," + value.getType().name() + ">";
        }
        if (field.getType() == FieldDescriptor.Type.ENUM) {
            return "ENUM:" + field.getEnumType().getFullName();
        }
        if (field.getType() == FieldDescriptor.Type.MESSAGE) {
            return "MESSAGE:" + field.getMessageType().getFullName();
        }
        return field.getType().name();
    }

    private static String cardinality(FieldDescriptor field) {
        if (field.getType() == FieldDescriptor.Type.MESSAGE
            && field.getMessageType().getOptions().getMapEntry()) {
            return "MAP";
        }
        return field.isRepeated() ? "REPEATED" : "SINGULAR";
    }

    private static String oneof(FieldDescriptor field) {
        return field.getContainingOneof() == null ? "-" : field.getContainingOneof().getName();
    }

    private static List<String> readBaseline(String resource) throws IOException {
        InputStream stream = ProtobufCompatibilityTest.class.getResourceAsStream(resource);
        assertNotNull("missing compatibility baseline " + resource, stream);
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } finally {
            reader.close();
        }
        return lines;
    }
}
