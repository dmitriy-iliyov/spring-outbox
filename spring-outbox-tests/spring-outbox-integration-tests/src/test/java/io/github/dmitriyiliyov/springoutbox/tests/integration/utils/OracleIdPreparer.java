package io.github.dmitriyiliyov.springoutbox.tests.integration.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

public class OracleIdPreparer implements IdPreparer {

    @Override
    public Object prepare(UUID id) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return bb.array();
    }
}
