/*
 * Copyright 2017 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.commons.mongodb.entities.subdocuments;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.util.Assert;
import uk.ac.ebi.eva.commons.core.models.IXref;
import uk.ac.ebi.eva.commons.mongodb.entities.AnnotationMongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lite version of genomic variant annotation generated using Ensembl VEP for indexing purposes.
 */
public class AnnotationIndexMongo {

    public static final String VEP_VERSION_FIELD = "vepv";

    public static final String VEP_CACHE_VERSION_FIELD = "cachev";

    public static final String SIFT_FIELD = "sift";

    public static final String POLYPHEN_FIELD = "polyphen";

    public static final String SO_ACCESSION_FIELD = "so";

    public static final String XREFS_FIELD = "xrefs";

    @Field(value = VEP_VERSION_FIELD)
    private String vepVersion;

    @Field(value = VEP_CACHE_VERSION_FIELD)
    private String vepCacheVersion;

    @Field(value = SIFT_FIELD)
    private List<Double> sifts;

    @Field(value = POLYPHEN_FIELD)
    private List<Double> polyphens;

    @Field(value = SO_ACCESSION_FIELD)
    private Set<Integer> soAccessions;

    @Field(value = XREFS_FIELD)
    private Set<String> xrefIds;

    AnnotationIndexMongo() {
        soAccessions = new HashSet<>();
        xrefIds = new HashSet<>();
    }

    /**
     * VariantWithSamplesAndAnnotation annotation constructor. Requires non empty values, otherwise throws {@link IllegalArgumentException}
     *
     * @param vepVersion
     * @param vepCacheVersion
     * @throws IllegalArgumentException If {@param vepVersion} or {@param vepCacheVersion} are null or empty values.
     */
    public AnnotationIndexMongo(String vepVersion, String vepCacheVersion) {
        Assert.hasText(vepVersion);
        Assert.hasText(vepCacheVersion);
        this.vepVersion = vepVersion;
        this.vepCacheVersion = vepCacheVersion;
        this.soAccessions = new HashSet<>();
        this.xrefIds = new HashSet<>();
    }

    /**
     * Private copy constructor
     *
     * @param annotationIndexMongo
     */
    private AnnotationIndexMongo(AnnotationIndexMongo annotationIndexMongo) {
        this(annotationIndexMongo.getVepVersion(), annotationIndexMongo.getVepCacheVersion());
        doConcatenate(annotationIndexMongo);
    }

    public AnnotationIndexMongo(AnnotationMongo annotation) {
        this(annotation.getVepVersion(), annotation.getVepCacheVersion());
        doConcatenate(annotation);
    }

    public AnnotationIndexMongo concatenate(AnnotationMongo annotation) {
        AnnotationIndexMongo temp = new AnnotationIndexMongo(this);
        temp.doConcatenate(annotation);
        return temp;
    }

    /**
     * Concatenate two VariantAnnotations in a new one. This method returns a new instance of AnnotationIndexMongo with
     * the concatenation of xrefIds and soAccessions. Polyphen and SIFT scores ranges need to be recalculated to store
     * the minimum and maximum across the 2 AnnotationIndex objects.
     *
     * @param annotation
     * @return
     */
    public AnnotationIndexMongo concatenate(AnnotationIndexMongo annotation) {
        AnnotationIndexMongo temp = new AnnotationIndexMongo(this);
        temp.doConcatenate(annotation);
        return temp;
    }

    private void doConcatenate(AnnotationIndexMongo annotationIndexMongo) {
        if (annotationIndexMongo.getXrefIds() != null) {
            addXrefIds(annotationIndexMongo.getXrefIds());
        }
        if (annotationIndexMongo.getSifts() != null) {
            annotationIndexMongo.getSifts().forEach(this::concatenateSiftRange);
        }
        if (annotationIndexMongo.getPolyphens() != null) {
            annotationIndexMongo.getPolyphens().forEach(this::concatenatePolyphenRange);
        }
        if (annotationIndexMongo.getSoAccessions() != null) {
            addSoAccessions(annotationIndexMongo.getSoAccessions());
        }
    }

    private void doConcatenate(AnnotationMongo annotation) {
        for (IXref xref : annotation.getXrefs()) {
            addXrefId(xref.getId());
        }
        for (ConsequenceTypeMongo consequenceType : annotation.getConsequenceTypes()) {
            final ScoreMongo sift = consequenceType.getSift();
            if (sift != null) {
                concatenateSiftRange(sift.getScore());
            }
            final ScoreMongo polyphen = consequenceType.getPolyphen();
            if (polyphen != null) {
                concatenatePolyphenRange(polyphen.getScore());
            }
            final Set<Integer> soAccessions = consequenceType.getSoAccessions();
            if (soAccessions != null) {
                addSoAccessions(soAccessions);
            }
        }
    }

    private Double maxOf(Collection<Double> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        return Collections.max(collection);
    }

    private Double minOf(Collection<Double> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        return Collections.min(collection);
    }

    private void concatenateRange(Collection<Double> collection, double score) {
        Double min = minOf(collection);
        Double max = maxOf(collection);
        if (min == null || max == null) {
            setRange(collection, score, score);
        } else if (score < min) {
            setRange(collection, score, max);
        } else if (score > max) {
            setRange(collection, min, score);
        }
    }

    private void setRange(Collection<Double> collection, double minScore, double maxScore) {
        collection.clear();
        collection.add(minScore);
        collection.add(maxScore);
    }

    private void concatenateSiftRange(double score) {
        if (sifts == null) {
            sifts = new ArrayList<>();
        }
        concatenateRange(sifts, score);
    }

    private void concatenatePolyphenRange(double score) {
        if (polyphens == null) {
            polyphens = new ArrayList<>();
        }
        concatenateRange(polyphens, score);
    }

    private void addXrefId(String id) {
        if (xrefIds == null) {
            xrefIds = new HashSet<>();
        }
        xrefIds.add(id);
    }

    private void addXrefIds(Set<String> ids) {
        if (xrefIds == null) {
            xrefIds = new HashSet<>();
        }
        xrefIds.addAll(ids);
    }

    private void addSoAccessions(Set<Integer> soAccessions) {
        if (this.soAccessions == null) {
            this.soAccessions = new HashSet<>();
        }
        this.soAccessions.addAll(soAccessions);
    }

    public List<Double> getSifts() {
        return sifts;
    }

    public List<Double> getPolyphens() {
        return polyphens;
    }

    public Set<Integer> getSoAccessions() {
        return soAccessions;
    }

    public Set<String> getXrefIds() {
        return xrefIds;
    }

    public String getVepVersion() {
        return vepVersion;
    }

    public String getVepCacheVersion() {
        return vepCacheVersion;
    }
}
