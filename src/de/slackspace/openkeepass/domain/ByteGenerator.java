package de.slackspace.openkeepass.domain;

public interface ByteGenerator {

    byte[] getRandomBytes(int numBytes);

}
