package com.capemod.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.capemod.server.controllers.Json;
import com.capemod.server.controllers.Multipart;
import com.capemod.server.services.ApiException;
import com.capemod.server.storage.CapeStorage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class JsonMultipartStorageTest {
    @Test public void parsesFlatJson() {
        Map<String, Object> m = Json.parseFlat("{\"a\":\"x\\\"y\",\"b\":null,\"c\":true,\"n\":12}");
        assertEquals("x\"y", m.get("a"));
        assertNull(m.get("b"));
        assertTrue(m.containsKey("b"));
        assertEquals(Boolean.TRUE, m.get("c"));
    }

    @Test public void rejectsBadJson() {
        String[] bad = {"", "{", "[]", "{\"a\":{}}", "{\"a\":1,}", "{\"a\":1}{", "{\"a\":1,\"a\":2}", "{a:1}", "{\"a\":\"\u0001\"}"};
        for (String s : bad) {
            try { Json.parseFlat(s); fail("deveria rejeitar: " + s); } catch (ApiException expected) { assertEquals(400, expected.status); }
        }
    }

    @Test public void quoteEscapes() {
        assertEquals("\"a\\\"b\\\\c\\n\"", Json.quote("a\"b\\c\n"));
    }

    @Test public void multipartBinarySafe() {
        byte[] data = new byte[300];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        byte[] head = ("--B1\r\nContent-Disposition: form-data; name=\"outro\"\r\n\r\nxx\r\n--B1\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"c.png\"\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1);
        byte[] tail = "\r\n--B1--\r\n".getBytes(StandardCharsets.ISO_8859_1);
        byte[] body = new byte[head.length + data.length + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(data, 0, body, head.length, data.length);
        System.arraycopy(tail, 0, body, head.length + data.length, tail.length);
        assertTrue(Arrays.equals(data, Multipart.extractFile(body, "multipart/form-data; boundary=B1")));
    }

    @Test public void multipartRejectsMissingFile() {
        byte[] body = "--B1\r\nContent-Disposition: form-data; name=\"x\"\r\n\r\nzz\r\n--B1--\r\n".getBytes(StandardCharsets.ISO_8859_1);
        try { Multipart.extractFile(body, "multipart/form-data; boundary=B1"); fail(); }
        catch (ApiException e) { assertEquals(400, e.status); }
    }

    @Test public void storageRejectsBadNamesAndIsIdempotent() throws Exception {
        CapeStorage st = new CapeStorage(Files.createTempDirectory("capemod-st"));
        String sha = "ab" + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd";
        assertEquals(64, sha.length());
        st.write(sha, new byte[]{1, 2, 3});
        st.write(sha, new byte[]{9});
        assertTrue(Arrays.equals(new byte[]{1, 2, 3}, st.read(sha)));
        st.delete(sha);
        assertNull(st.read(sha));
        try { st.write("../../evil", new byte[1]); fail(); } catch (IllegalArgumentException expected) { }
    }
}
