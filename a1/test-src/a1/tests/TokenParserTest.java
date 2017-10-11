package a1.tests;

import a1.util.TokenParser;

import static org.junit.jupiter.api.Assertions.*;

class TokenParserTest
{
    @org.junit.jupiter.api.Test
    void parseAsInt()
    {
        // Should throw on null argument
        assertThrows(NullPointerException.class, () -> TokenParser.parseAsInt(null) );
        // Test if a valid string is parsed correctly
        assertEquals(TokenParser.parseAsInt("51"), 51);
        // Should throw on getting a non-integer number
        assertThrows(IllegalArgumentException.class, () -> TokenParser.parseAsInt("51.2") );
        // Should throw on exceeding bounds
        assertThrows(IllegalArgumentException.class, () -> TokenParser.parseAsInt("51", 1, 10) );

    }

    @org.junit.jupiter.api.Test
    void parseAsFile()
    {
        // Tests that the method throws on null argument
        assertThrows(NullPointerException.class, () -> TokenParser.parseAsPath(null));
        // Non-existant file should throw
        assertThrows(IllegalArgumentException.class, () -> TokenParser.parseAsPath("1.txt"));
        // A file that exists and is readable should return true
        assertTrue(TokenParser.parseAsPath(getClass().getResource("good_config.txt").getPath()).canRead());
    }

}