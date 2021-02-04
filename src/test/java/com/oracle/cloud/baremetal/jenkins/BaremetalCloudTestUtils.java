package com.oracle.cloud.baremetal.jenkins;

import java.util.Collection;
import java.util.TreeSet;

import hudson.model.labels.LabelAtom;
import hudson.util.QuotedStringTokenizer;
import jenkins.model.Jenkins;

public class BaremetalCloudTestUtils {

    /**
     * Similar to {@link hudson.model.Label#parse} but does not use
     * {@link Jenkins#getInstanceOrNull()}.
     */
    public static Collection<LabelAtom> parseLabels(String labels) {
        Collection<LabelAtom> result = new TreeSet<>();
        if (labels != null && !labels.isEmpty()) {
            for (QuotedStringTokenizer tok = new QuotedStringTokenizer(labels); tok.hasMoreTokens();) {
                result.add(new LabelAtom(tok.nextToken()));
            }
        }
        return result;
    }
}
