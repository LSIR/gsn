package ch.epfl.gsn.metadata.tools.taxonomy;

import com.csvreader.CsvReader;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

/**
 * Created by kryvych on 16/03/15.
 */
@Named
public class TaxonomyLoader {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private TaxonomyRepository repository;

    @Inject
    public TaxonomyLoader(TaxonomyRepository repository) {
        this.repository = repository;
    }

    public long loadTaxonomy(String fileName) {
        long count = 0;
        try (FileReader reader = new FileReader(fileName)) {
            Set<TaxonomyProperty> terms = readTaxonomy(reader);
            repository.deleteAll();
            repository.save(terms);

            count = repository.count();

        } catch (IOException e) {
            logger.error("Cannot read taxonomy values from " + fileName, e);
        }
        return count;
    }

    public Set<TaxonomyProperty> readTaxonomy(FileReader reader) throws IOException {
        Set<TaxonomyProperty> result = Sets.newHashSet();

        CsvReader csvReader = new CsvReader(reader);
        while (csvReader.readRecord()) {
            TaxonomyProperty property = new TaxonomyProperty(csvReader.get(0).trim(),
                    csvReader.get(2).trim().toLowerCase(), csvReader.get(3).trim().toLowerCase());
            result.add(property);

        }

        return result;
    }

}
