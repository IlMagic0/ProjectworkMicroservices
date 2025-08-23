package io.datajek.spring.basics.client;

import org.springframework.web.multipart.MultipartFile;

public class ByteArrayMultipartFile implements MultipartFile {
    private final byte[] bytes;
    private final String name;
    private final String contentType;

    public ByteArrayMultipartFile(byte[] bytes, String name, String contentType) {
        this.bytes = bytes;
        this.name = name;
        this.contentType = contentType;
    }

    @Override public String getName() { return name; }
    @Override public String getOriginalFilename() { return name; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return bytes == null || bytes.length == 0; }
    @Override public long getSize() { return bytes.length; }
    @Override public byte[] getBytes() { return bytes; }
    @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
    @Override public void transferTo(java.io.File dest) { throw new UnsupportedOperationException(); }
}

