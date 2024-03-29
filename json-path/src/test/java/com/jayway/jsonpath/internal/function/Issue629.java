package com.jayway.jsonpath.internal.function;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class Issue629 {
    @Test
    public void testUncloseParenthesis() throws IOException {
        try {
            JsonPath jsonPath = JsonPath.compile("$.A.B.C.D(");
            fail("accepted jsonpath with unclosed parentheses");
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("Arguments to function:"));
        }
    }

    @Test
    public void testUncloseParenthesisWithNestedCall() throws IOException {
        try {
            JsonPath jsonPath = JsonPath.compile("$.A.B.C.sum(D()");
            fail("accepted jsonpath with unclosed parentheses");
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("Arguments to function:"));
        }
    }
}
