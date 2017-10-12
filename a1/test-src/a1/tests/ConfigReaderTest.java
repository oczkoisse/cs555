package a1.tests;

import a1.util.ConfigReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigReaderTest
{
    @Test
    void parseFile()
    {
        // Should throw NullPointerException irrespective of ignoreErrors if path is null
        assertThrows(NullPointerException.class, () -> ConfigReader.read(null, true, true));

        // Should throw IllegalArgumentException irrespective of ignoreErrors if path is inaccessible
        assertThrows(IllegalArgumentException.class, () -> ConfigReader.read("invisible_file.txt", true, true));

        // Should be parsed as well as resolved
        assertNotNull(ConfigReader.read(getClass().getResource("/good_config.txt").getPath(), false, true));

        // Should fail to resolve because of hostname
        assertNull(ConfigReader.read(getClass().getResource("/bad_config_1.txt").getPath(), false, true));

        // Should fail to parse because of illegal port number
        assertNull(ConfigReader.read(getClass().getResource("/bad_config_2.txt").getPath(), false, false));

        // Should fail to parse because of multiple colons
        assertNull(ConfigReader.read(getClass().getResource("/bad_config_3.txt").getPath(), false, false));
    }

}