package ch.epfl.gsn.metadata.core.services;

import ch.epfl.gsn.metadata.core.model.exchange.ExchangeMetadata;
import ch.epfl.gsn.metadata.core.repositories.ExchangeMetadataRepository;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Created by kryvych on 03/09/15.
 */
@Named
public class ExchangeMatadataService {

    private ExchangeMetadataRepository repository;

    protected static final Logger logger = LoggerFactory.getLogger(ExchangeMatadataService.class);

    @Inject
    public ExchangeMatadataService(ExchangeMetadataRepository repository) {
        this.repository = repository;
    }

    public int write(Path directory) {
        int count = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                String name = FilenameUtils.removeExtension(file.getFileName().toString().toLowerCase());
                String content = readFile(file, Charset.defaultCharset());
                ExchangeMetadata metadata = new ExchangeMetadata(name, content);
                repository.save(metadata);
                count++;
            }
        } catch (IOException e) {
            logger.error("Problem reading metadata file ", e);
        }

        return count;
    }

    protected String readFile(Path path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded, encoding);
    }
}
