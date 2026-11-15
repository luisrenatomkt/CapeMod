package com.capemod.server.controllers;

import com.capemod.server.services.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Extrai o campo "file" de um corpo multipart/form-data (binario seguro, sem dependencias). */
public final class Multipart {
    private Multipart() {}

    public static byte[] extractFile(byte[] body, String contentType) {
        String boundary = boundary(contentType);
        byte[] delim = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] nextDelim = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        int pos = indexOf(body, delim, 0);
        while (pos >= 0) {
            int p = pos + delim.length;
            if (p + 1 < body.length && body[p] == '-' && body[p + 1] == '-') break; // delimitador final
            if (p + 1 >= body.length || body[p] != '\r' || body[p + 1] != '\n') break;
            p += 2;
            int headersEnd = indexOf(body, new byte[]{'\r', '\n', '\r', '\n'}, p);
            if (headersEnd < 0) break;
            String headers = new String(body, p, headersEnd - p, StandardCharsets.ISO_8859_1);
            int dataStart = headersEnd + 4;
            int dataEnd = indexOf(body, nextDelim, dataStart);
            if (dataEnd < 0) break;
            if (headers.toLowerCase().contains("name=\"file\"")) {
                return Arrays.copyOfRange(body, dataStart, dataEnd);
            }
            pos = dataEnd + 2; // aponta para o "--boundary" seguinte
        }
        throw new ApiException(400, "Campo 'file' ausente ou multipart malformado.");
    }

    private static String boundary(String contentType) {
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            throw new ApiException(415, "Use multipart/form-data.");
        }
        int i = contentType.toLowerCase().indexOf("boundary=");
        if (i < 0) throw new ApiException(400, "Boundary ausente.");
        String b = contentType.substring(i + 9).trim();
        int semi = b.indexOf(';');
        if (semi >= 0) b = b.substring(0, semi).trim();
        if (b.length() >= 2 && b.startsWith("\"") && b.endsWith("\"")) b = b.substring(1, b.length() - 1);
        if (b.isEmpty() || b.length() > 70 || !b.matches("[0-9A-Za-z'()+_,./:=?-]+")) {
            throw new ApiException(400, "Boundary inválido.");
        }
        return b;
    }

    static int indexOf(byte[] hay, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (hay[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }
}
