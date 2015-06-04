package ch.epfl.gsn.metadata.tools.taxonomy;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;

/**
 * Created by kryvych on 16/03/15.
 */
@Document(collection = "taxonomy")
public class TaxonomyProperty {

    @Id
    private BigInteger id;

    private String columnName;
    private String media;
    private String taxonomyName;

    public TaxonomyProperty(String columnName, String media, String taxonomyName) {
        this.columnName = columnName;
        this.media = media;
        this.taxonomyName = taxonomyName;
    }

    public BigInteger getId() {
        return id;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getMedia() {
        return media;
    }

    public String getTaxonomyName() {
        return taxonomyName;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }
}

