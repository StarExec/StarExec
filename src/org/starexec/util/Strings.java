/*
 * Copyright (c) 2006, Stephen Kelvin Friedrich,  All rights reserved.
 *
 * This a BSD license. If you use or enhance the code, I'd be pleased if you sent a mail to s.friedrich@eekboom.com
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
         following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of the "Stephen Kelvin Friedrich" nor the names of its contributors may be used to endorse
 *       or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Modified by Todd Elvers
 */

package org.starexec.util;


import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;

import org.starexec.data.database.Jobs;
import org.starexec.data.database.Statistics;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;

/**
 * Utility class for common String operations
 */
public final class Strings {
    /**
     * <p>A string comparator that does case sensitive comparisons and handles embedded numbers correctly.</p>
     * <p><b>Do not use</b> if your app might ever run on any locale that uses more than 7-bit ascii characters.</p>
     */
    private static final Comparator<String> NATURAL_COMPARATOR_ASCII = new Comparator<String>() {
        public int compare(String o1, String o2) {
            return compareNaturalAscii(o1, o2);
        }
    };

    /**
     * <p>A string comparator that does case insensitive comparisons and handles embedded numbers correctly.</p>
     * <p><b>Do not use</b> if your app might ever run on any locale that uses more than 7-bit ascii characters.</p>
     */
    private static final Comparator<String> IGNORE_CASE_NATURAL_COMPARATOR_ASCII = new Comparator<String>() {
        public int compare(String o1, String o2) {
            return compareNaturalIgnoreCaseAscii(o1, o2);
        }
    };

    /**
     * This is a utility class (static methods only), don't instantiate.
     */
    private Strings() {
    }

    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A string comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<String> getNaturalComparator() {
        Collator collator = Collator.getInstance();
        return getNaturalComparator(collator);
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A Benchmark comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<Benchmark> getNaturalComparatorForBenchmarks(int sortColumnIndex) {
        Collator collator = Collator.getInstance();
        return getNaturalComparatorForBenchmarks(collator, sortColumnIndex);
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A Solver comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<Solver> getNaturalComparatorForSolvers(int sortColumnIndex) {
        Collator collator = Collator.getInstance();
        return getNaturalComparatorForSolvers(collator, sortColumnIndex);
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A User comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<User> getNaturalComparatorForUsers(int sortColumnIndex) {
        Collator collator = Collator.getInstance();
        return getNaturalComparatorForUsers(collator, sortColumnIndex);
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A Job comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<Job> getNaturalComparatorForJobs(int sortColumnIndex, int spaceId) {
        Collator collator = Collator.getInstance();
        return getNaturalComparatorForJobs(collator, sortColumnIndex, spaceId);
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the current locale's order rules.
     * <p>For example in German locale this will be a comparator that handles umlauts correctly and ignores
     * upper/lower case differences.</p>
     *
     * @return <p>A Space comparator that uses the current locale's order rules and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<Space> getNaturalComparatorForSpaces(int sortColumnIndex) {
        Collator collator = Collator.getInstance();
        return getNaturalComparatorForSpaces(collator, sortColumnIndex);
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number) subwords - must not be null
     * @return <p>A Space comparator that uses the given Collator to compare subwords and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator()
     */
    public static Comparator<Space> getNaturalComparatorForSpaces(final Collator collator, final int sortColumnIndex) {
        if(collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<Space>() {
            public int compare(Space s1, Space s2) {
            	if(0 == sortColumnIndex){
            		return compareNatural(collator, s1.getName(), s2.getName());
            	} else {
            		return compareNatural(collator, s1.getDescription(), s2.getDescription());
            	}
            }
        };
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number) subwords - must not be null
     * @return <p>A Job comparator that uses the given Collator to compare subwords and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator()
     */
    public static Comparator<Job> getNaturalComparatorForJobs(final Collator collator, final int sortColumnIndex, final int spaceId) {
        if(collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<Job>() {
        	HashMap<Integer, HashMap<String, String>> jp = Statistics.getJobPairOverviews(Jobs.getBySpace(spaceId));
            public int compare(Job j1, Job j2) {
            	if(0 == sortColumnIndex){			// Sort on 'name'
            		return compareNatural(collator, j1.getName(), j2.getName());
            	} else if (1 == sortColumnIndex) {	// Sort on 'status'
            		return compareNatural(collator, jp.get(j1.getId()).get("pendingPairs"), jp.get(j2.getId()).get("pendingPairs"));
            	} else if (2 == sortColumnIndex) {	// Sort on 'complete'
            		return compareNatural(collator, jp.get(j1.getId()).get("completePairs"), jp.get(j2.getId()).get("completePairs"));
            	} else if (3 == sortColumnIndex) {	// Sort on 'pending'
            		return compareNatural(collator, jp.get(j1.getId()).get("pendingPairs"), jp.get(j2.getId()).get("pendingPairs"));
            	} else  {							// Sort on 'error'
            		return compareNatural(collator, jp.get(j1.getId()).get("errorPairs"), jp.get(j2.getId()).get("errorPairs"));
            	}
            }
        };
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number) subwords - must not be null
     * @return <p>A User comparator that uses the given Collator to compare subwords and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator()
     */
    public static Comparator<User> getNaturalComparatorForUsers(final Collator collator, final int sortColumnIndex) {
        if(collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<User>() {
            public int compare(User u1, User u2) {
            	if(0 == sortColumnIndex){
            		return compareNatural(collator, u1.getFullName(), u2.getFullName());
            	} else if (1 == sortColumnIndex) {
            		return compareNatural(collator, u1.getInstitution(), u2.getInstitution());
            	} else {
            		return compareNatural(collator, u1.getEmail(), u2.getEmail());
            	}
            }
        };
    }
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number) subwords - must not be null
     * @return <p>A Benchmark comparator that uses the given Collator to compare subwords and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator()
     */
    public static Comparator<Benchmark> getNaturalComparatorForBenchmarks(final Collator collator, final int sortColumnIndex) {
        if(collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<Benchmark>() {
            public int compare(Benchmark b1, Benchmark b2) {
            	if(0 == sortColumnIndex){
            		return compareNatural(collator, b1.getName(), b2.getName());
            	} else {
            		return compareNatural(collator, b1.getType().getName(), b2.getType().getName());
            	}
            }
        };
    }

    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number) subwords - must not be null
     * @return <p>A Solver comparator that uses the given Collator to compare subwords and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator()
     */
    public static Comparator<Solver> getNaturalComparatorForSolvers(final Collator collator, final int sortColumnIndex) {
        if(collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<Solver>() {
            public int compare(Solver s1, Solver s2) {
            	if(0 == sortColumnIndex){
            		return compareNatural(collator, s1.getName(), s2.getName());
            	} else {
            		return compareNatural(collator, s1.getDescription(), s2.getDescription());
            	}
            }
        };
    }
    
    
    
    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * using the given collator.
     *
     * @param collator used for locale specific comparison of text (non-number) subwords - must not be null
     * @return <p>A string comparator that uses the given Collator to compare subwords and handles embedded numbers
     *         correctly.</p>
     * @see #getNaturalComparator()
     */
    public static Comparator<String> getNaturalComparator(final Collator collator) {
        if(collator == null) {
            // it's important to explicitly handle this here - else the bug will manifest anytime later in possibly
            // unrelated code that tries to use the comparator
            throw new NullPointerException("collator must not be null");
        }
        return new Comparator<String>() {
            public int compare(String o1, String o2) {
                return compareNatural(collator, o1, o2);
            }
        };
    }

    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * based on each character's Unicode value.
     *
     * @return <p>a string comparator that does case sensitive comparisons on pure ascii strings and handles embedded
     *         numbers correctly.</p>
     *         <b>Do not use</b> if your app might ever run on any locale that uses more than 7-bit ascii characters.
     * @see #getNaturalComparator()
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<String> getNaturalComparatorAscii() {
        return NATURAL_COMPARATOR_ASCII;
    }

    /**
     * Returns a comparator that compares contained numbers based on their numeric values and compares other parts
     * based on each character's Unicode value while ignore upper/lower case differences.
     * <b>Do not use</b> if your app might ever run on any locale that uses more than 7-bit ascii characters.
     *
     * @return <p>a string comparator that does case insensitive comparisons on pure ascii strings and handles embedded
     *         numbers correctly.</p>
     * @see #getNaturalComparator()
     * @see #getNaturalComparator(java.text.Collator)
     */
    public static Comparator<String> getNaturalComparatorIgnoreCaseAscii() {
        return IGNORE_CASE_NATURAL_COMPARATOR_ASCII;
    }

    /**
     * <p>Compares two strings using the current locale's rules and comparing contained numbers based on their numeric
     * values.</p>
     * <p>This is probably the best default comparison to use.</p>
     * <p>If you know that the texts to be compared are in a certain language that differs from the default locale's
     * langage, then get a collator for the desired locale ({@link java.text.Collator#getInstance(java.util.Locale)})
     * and pass it to {@link #compareNatural(java.text.Collator, String, String)}</p>
     *
     * @param s first string
     * @param t second string
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    public static int compareNatural(String s, String t) {
        return compareNatural(s, t, false, Collator.getInstance());
    }

    /**
     * <p>Compares two strings using the given collator and comparing contained numbers based on their numeric
     * values.</p>
     *
     * @param s first string
     * @param t second string
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    public static int compareNatural(Collator collator, String s, String t) {
        return compareNatural(s, t, true, collator);
    }

    /**
     * <p>Compares two strings using each character's Unicode value for non-digit characters and the numeric values off
     * any contained numbers.</p>
     * <p>(This will probably make sense only for strings containing 7-bit ascii characters only.)</p>
     *
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    public static int compareNaturalAscii(String s, String t) {
        return compareNatural(s, t, true, null);
    }

    /**
     * <p>Compares two strings using each character's Unicode value - ignoring upper/lower case - for non-digit
     * characters and the numeric values of any contained numbers.</p>
     * <p>(This will probably make sense only for strings containing 7-bit ascii characters only.)</p>
     *
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    public static int compareNaturalIgnoreCaseAscii(String s, String t) {
        return compareNatural(s, t, false, null);
    }

    /**
     * @param s             first string
     * @param t             second string
     * @param caseSensitive treat characters differing in case only as equal - will be ignored if a collator is given
     * @param collator      used to compare subwords that aren't numbers - if null, characters will be compared
     *                      individually based on their Unicode value
     * @return zero iff <code>s</code> and <code>t</code> are equal,
     *         a value less than zero iff <code>s</code> lexicographically precedes <code>t</code>
     *         and a value larger than zero iff <code>s</code> lexicographically follows <code>t</code>
     */
    private static int compareNatural(String s, String t, boolean caseSensitive, Collator collator) {
        int sIndex = 0;
        int tIndex = 0;

        int sLength = s.length();
        int tLength = t.length();

        while(true) {
            // both character indices are after a subword (or at zero)

            // Check if one string is at end
            if(sIndex == sLength && tIndex == tLength) {
                return 0;
            }
            if(sIndex == sLength) {
                return -1;
            }
            if(tIndex == tLength) {
                return 1;
            }

            // Compare sub word
            char sChar = s.charAt(sIndex);
            char tChar = t.charAt(tIndex);

            boolean sCharIsDigit = Character.isDigit(sChar);
            boolean tCharIsDigit = Character.isDigit(tChar);

            if(sCharIsDigit && tCharIsDigit) {
                // Compare numbers

                // skip leading 0s
                int sLeadingZeroCount = 0;
                while(sChar == '0') {
                    ++sLeadingZeroCount;
                    ++sIndex;
                    if(sIndex == sLength) {
                        break;
                    }
                    sChar = s.charAt(sIndex);
                }
                int tLeadingZeroCount = 0;
                while(tChar == '0') {
                    ++tLeadingZeroCount;
                    ++tIndex;
                    if(tIndex == tLength) {
                        break;
                    }
                    tChar = t.charAt(tIndex);
                }
                boolean sAllZero = sIndex == sLength || !Character.isDigit(sChar);
                boolean tAllZero = tIndex == tLength || !Character.isDigit(tChar);
                if(sAllZero && tAllZero) {
                    continue;
                }
                if(sAllZero && !tAllZero) {
                    return -1;
                }
                if(tAllZero) {
                    return 1;
                }

                int diff = 0;
                do {
                    if(diff == 0) {
                        diff = sChar - tChar;
                    }
                    ++sIndex;
                    ++tIndex;
                    if(sIndex == sLength && tIndex == tLength) {
                        return diff != 0 ? diff : sLeadingZeroCount - tLeadingZeroCount;
                    }
                    if(sIndex == sLength) {
                        if(diff == 0) {
                            return -1;
                        }
                        return Character.isDigit(t.charAt(tIndex)) ? -1 : diff;
                    }
                    if(tIndex == tLength) {
                        if(diff == 0) {
                            return 1;
                        }
                        return Character.isDigit(s.charAt(sIndex)) ? 1 : diff;
                    }
                    sChar = s.charAt(sIndex);
                    tChar = t.charAt(tIndex);
                    sCharIsDigit = Character.isDigit(sChar);
                    tCharIsDigit = Character.isDigit(tChar);
                    if(!sCharIsDigit && !tCharIsDigit) {
                        // both number sub words have the same length
                        if(diff != 0) {
                            return diff;
                        }
                        break;
                    }
                    if(!sCharIsDigit) {
                        return -1;
                    }
                    if(!tCharIsDigit) {
                        return 1;
                    }
                } while(true);
            }
            else {
                // Compare words
                if(collator != null) {
                    // To use the collator the whole subwords have to be compared - character-by-character comparision
                    // is not possible. So find the two subwords first
                    int aw = sIndex;
                    int bw = tIndex;
                    do {
                        ++sIndex;
                    } while(sIndex < sLength && !Character.isDigit(s.charAt(sIndex)));
                    do {
                        ++tIndex;
                    } while(tIndex < tLength && !Character.isDigit(t.charAt(tIndex)));

                    String as = s.substring(aw, sIndex);
                    String bs = t.substring(bw, tIndex);
                    int subwordResult = collator.compare(as, bs);
                    if(subwordResult != 0) {
                        return subwordResult;
                    }
                }
                else {
                    // No collator specified. All characters should be ascii only. Compare character-by-character.
                    do {
                        if(sChar != tChar) {
                            if(caseSensitive) {
                                return sChar - tChar;
                            }
                            sChar = Character.toUpperCase(sChar);
                            tChar = Character.toUpperCase(tChar);
                            if(sChar != tChar) {
                                sChar = Character.toLowerCase(sChar);
                                tChar = Character.toLowerCase(tChar);
                                if(sChar != tChar) {
                                    return sChar - tChar;
                                }
                            }
                        }
                        ++sIndex;
                        ++tIndex;
                        if(sIndex == sLength && tIndex == tLength) {
                            return 0;
                        }
                        if(sIndex == sLength) {
                            return -1;
                        }
                        if(tIndex == tLength) {
                            return 1;
                        }
                        sChar = s.charAt(sIndex);
                        tChar = t.charAt(tIndex);
                        sCharIsDigit = Character.isDigit(sChar);
                        tCharIsDigit = Character.isDigit(tChar);
                    } while(!sCharIsDigit && !tCharIsDigit);
                }
            }
        }
    }
}
