package ch.epfl.gsn.metadata.tools.gsn;

import ch.epfl.gsn.metadata.core.services.VirtualSensorPersistenceService;
import ch.epfl.gsn.metadata.mongodb.MongoApplicationConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by kryvych on 11/03/15.
 */
public class GSNImportTool {

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 2) {
            System.out.println("Arguments: fileLocation serverURL");
            System.exit(1);
        }

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MongoApplicationConfig.class);
        ctx.scan("ch.epfl.gsn.metadata");
        VirtualSensorPersistenceService service = ctx.getBean(VirtualSensorPersistenceService.class);

        InputStream fileStream = new FileInputStream(args[0]);

        int count = service.writeVirtualSensors(fileStream, args[1]);
        System.out.println("count = " + count);
    }

}
