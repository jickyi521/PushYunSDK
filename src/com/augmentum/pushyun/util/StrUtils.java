package com.augmentum.pushyun.util;

/**
 * General-purpose string utilities.
 */
public class StrUtils
{
    /**
     * Tests whether the given string is null or has zero length.
     * 
     * @param str String to test.
     * @return true when the given string is null or zero length; false otherwise.
     */
    public static boolean isEmpty(String str)
    {
        return (str == null) || (str.length() == 0);
    }
}
