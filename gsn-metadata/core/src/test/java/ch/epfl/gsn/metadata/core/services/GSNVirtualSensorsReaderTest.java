package ch.epfl.gsn.metadata.core.services;

import ch.epfl.gsn.metadata.core.model.gsnjson.VirtualSensor;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class GSNVirtualSensorsReaderTest {

    private GSNVirtualSensorsReader subject;

    @Before
    public void init() {
        subject = new GSNVirtualSensorsReader();
    }

    @Test
    public void readTest() throws Exception {
        String fileName = "/Users/kryvych/Projects/gsn_meta_service/core/src/test/resources/gsn_sensors.json";
        InputStream fileStream = new FileInputStream(fileName);

        List<VirtualSensor> virtualSensors = subject.read(fileStream);
        System.out.println("virtualSensors.size() = " + virtualSensors.size());

    }
}