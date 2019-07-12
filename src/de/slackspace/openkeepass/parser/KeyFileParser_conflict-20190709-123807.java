package de.slackspace.openkeepass.parser;

import de.slackspace.openkeepass.domain.KeyFileBytes;

public interface KeyFileParser {

    KeyFileBytes readKeyFile(byte[] keyFile);
}
